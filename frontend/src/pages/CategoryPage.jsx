import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  getCategories,
  getCategory,
  getTopScores,
  getFilteredScores,
  getBaselines,
  getCorrelation,
  getGlobalDataset,
  getUserCategoryTopScore,
  compareGlobalStat,
  submitScore,
  adminDeleteCategory,
  adminDeleteScore,
} from '../api';

const MAX_SCORE = 999999999999;
const MIN_SCORE = 0;
const CHART_SAMPLE_SIZE = 1000;
const HISTOGRAM_BIN_COUNT = 12;

const clamp = (value, min, max) => Math.min(Math.max(value, min), max);

const createFilterTags = (region, sex) => {
  const tags = {};
  if (region) tags.region = region;
  if (sex) tags.sex = sex;
  return tags;
};

const getCategoryImage = (category) => category?.imageData || category?.image_data || '';

const isGlobalCategory = (category) => Boolean(category?.isGlobal) || category?.categoryScope === 'GLOBAL';
const isNhanesCategory = (category) => String(category?.globalSourceKey || '').startsWith('nhanes_');
const isHeightCategory = (category) => category?.globalSourceKey === 'height';

const buildHistogram = (values, preferredBinCount = HISTOGRAM_BIN_COUNT) => {
  if (values.length === 0) return [];

  const min = Math.min(...values);
  const max = Math.max(...values);
  const binCount = Math.min(preferredBinCount, Math.max(values.length, 1));

  if (min === max) {
    return [{ start: min, end: max, count: values.length }];
  }

  const binSize = (max - min) / binCount;
  const bins = Array.from({ length: binCount }, (_, index) => ({
    start: min + binSize * index,
    end: index === binCount - 1 ? max : min + binSize * (index + 1),
    count: 0,
  }));

  values.forEach((value) => {
    const index = value === max ? binCount - 1 : Math.floor((value - min) / binSize);
    bins[clamp(index, 0, binCount - 1)].count += 1;
  });

  return bins;
};

const buildCompetitionRankByScore = (entries) => {
  const rankByScore = new Map();

  entries.forEach((entry, index) => {
    const score = Number(entry.score);
    if (!Number.isFinite(score)) return;
    if (!rankByScore.has(score)) {
      rankByScore.set(score, index + 1);
    }
  });

  return rankByScore;
};

const buildPercentilePoints = (values) => {
  const sortedValues = values
    .map((value) => Number(value))
    .filter((value) => Number.isFinite(value))
    .sort((a, b) => a - b);

  if (sortedValues.length === 0) return [];
  if (sortedValues.length === 1) return [{ percentile: 50, value: sortedValues[0] }];

  return sortedValues.map((value, index) => ({
    percentile: (index / (sortedValues.length - 1)) * 100,
    value,
  }));
};

const isValidScore = (val, minLimit = MIN_SCORE, maxLimit = MAX_SCORE) => {
  if (val === '' || val === null || val === undefined) return false;
  if (/e/i.test(String(val))) return false;
  const num = parseFloat(val);
  if (isNaN(num)) return false;
  if (num < minLimit || num > maxLimit) return false;
  return true;
};

export default function CategoryPage({ currentUser }) {
  const { categoryId } = useParams();
  const navigate = useNavigate();
  const isAdmin = Boolean(currentUser?.admin);
  const [adminMenuOpen, setAdminMenuOpen] = useState(false);
  const [adminError, setAdminError] = useState('');
  const [category, setCategory] = useState(null);
  const [scores, setScores] = useState([]);
  const [baselines, setBaselines] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [chartScores, setChartScores] = useState([]);
  const [chartTotalElements, setChartTotalElements] = useState(0);
  const [chartLoading, setChartLoading] = useState(false);
  const [chartError, setChartError] = useState('');
  const [globalDataset, setGlobalDataset] = useState(null);
  const [globalComparison, setGlobalComparison] = useState(null);
  const [localCategoryOptions, setLocalCategoryOptions] = useState([]);
  const [correlationCategoryId, setCorrelationCategoryId] = useState('');
  const [correlationData, setCorrelationData] = useState(null);
  const [correlationLoading, setCorrelationLoading] = useState(false);
  const [correlationError, setCorrelationError] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [filterRegion, setFilterRegion] = useState('');
  const [filterSex, setFilterSex] = useState('');
  const [appliedFilterRegion, setAppliedFilterRegion] = useState('');
  const [appliedFilterSex, setAppliedFilterSex] = useState('');
  const [filterRequestId, setFilterRequestId] = useState(0);
  const [globalFilterSex, setGlobalFilterSex] = useState('');
  const [globalFilterAgeGroup, setGlobalFilterAgeGroup] = useState('');
  const [globalFilterRegion, setGlobalFilterRegion] = useState('');
  const [appliedGlobalFilterSex, setAppliedGlobalFilterSex] = useState('');
  const [appliedGlobalFilterAgeGroup, setAppliedGlobalFilterAgeGroup] = useState('');
  const [appliedGlobalFilterRegion, setAppliedGlobalFilterRegion] = useState('');

  const [scoreValue, setScoreValue] = useState('');
  const [anonymous, setAnonymous] = useState(false);
  const [latestSubmittedScore, setLatestSubmittedScore] = useState(null);
  const [submitError, setSubmitError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const loadCategory = useCallback(async () => {
    try {
      const cat = await getCategory(categoryId);
      setCategory(cat);
    } catch (err) {
      setError(err.message);
    }
  }, [categoryId]);

  const getFilterTags = useCallback(() => {
    return createFilterTags(appliedFilterRegion, appliedFilterSex);
  }, [appliedFilterRegion, appliedFilterSex]);

  const getGlobalFilterTags = useCallback(() => {
    const tags = {};
    if (appliedGlobalFilterSex) tags.sex = appliedGlobalFilterSex;
    if (appliedGlobalFilterAgeGroup) tags.age_group = appliedGlobalFilterAgeGroup;
    if (appliedGlobalFilterRegion) tags.region = appliedGlobalFilterRegion;
    return tags;
  }, [appliedGlobalFilterSex, appliedGlobalFilterAgeGroup, appliedGlobalFilterRegion]);

  const loadScores = useCallback(async (currentPage = 0) => {
    try {
      let data;
      const tags = getFilterTags();

      const hasFilters = Object.keys(tags).length > 0;

      if (hasFilters) {
        data = await getFilteredScores(categoryId, tags, currentPage, 25);
      } else {
        data = await getTopScores(categoryId, currentPage, 25);
      }

      setScores(data.scores || []);
      setPage(data.page);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (err) {
      setError(err.message);
    }
  }, [categoryId, getFilterTags]);

  const loadChartScores = useCallback(async () => {
    setChartLoading(true);
    setChartError('');

    try {
      let data;
      const tags = getFilterTags();
      const hasFilters = Object.keys(tags).length > 0;

      if (hasFilters) {
        data = await getFilteredScores(categoryId, tags, 0, CHART_SAMPLE_SIZE);
      } else {
        data = await getTopScores(categoryId, 0, CHART_SAMPLE_SIZE);
      }

      const validScores = (data.scores || []).filter((entry) => {
        const score = Number(entry.score);
        return Number.isFinite(score);
      });

      setChartScores(validScores);
      setChartTotalElements(data.totalElements ?? validScores.length);
    } catch (err) {
      setChartScores([]);
      setChartTotalElements(0);
      setChartError(err.message || 'Failed to load score distribution');
    } finally {
      setChartLoading(false);
    }
  }, [categoryId, getFilterTags]);

  const loadGlobalDataset = useCallback(async () => {
    setChartLoading(true);
    setChartError('');

    try {
      const data = await getGlobalDataset(categoryId, getGlobalFilterTags());
      setGlobalDataset(data);
      setChartTotalElements(data.sampleSize ?? 0);
    } catch (err) {
      setGlobalDataset(null);
      setChartTotalElements(0);
      setChartError(err.message || 'Failed to load global dataset');
    } finally {
      setChartLoading(false);
    }
  }, [categoryId, getGlobalFilterTags]);

  const loadBaselines = useCallback(async () => {
    try {
      const data = await getBaselines(categoryId);
      setBaselines(data || []);
    } catch {
      // baselines are optional
    }
  }, [categoryId]);

  const loadCorrelation = useCallback(async () => {
    if (!category || isGlobalCategory(category) || !correlationCategoryId) {
      setCorrelationData(null);
      setCorrelationError('');
      return;
    }

    setCorrelationLoading(true);
    setCorrelationError('');
    try {
      const data = await getCorrelation(categoryId, correlationCategoryId);
      setCorrelationData(data);
    } catch (err) {
      setCorrelationData(null);
      setCorrelationError(err.message || 'Failed to load correlation data');
    } finally {
      setCorrelationLoading(false);
    }
  }, [category, categoryId, correlationCategoryId]);

  useEffect(() => {
    setLoading(true);
    setCategory(null);
    setScores([]);
    setBaselines([]);
    setChartScores([]);
    setGlobalDataset(null);
    setGlobalComparison(null);
    setLatestSubmittedScore(null);
    setLocalCategoryOptions([]);
    setCorrelationCategoryId('');
    setCorrelationData(null);
    setCorrelationError('');
    setGlobalFilterSex('');
    setGlobalFilterAgeGroup('');
    setGlobalFilterRegion('');
    setAppliedGlobalFilterSex('');
    setAppliedGlobalFilterAgeGroup('');
    setAppliedGlobalFilterRegion('');
    loadCategory().finally(() => setLoading(false));
  }, [loadCategory]);

  useEffect(() => {
    if (!category || isGlobalCategory(category)) {
      setLocalCategoryOptions([]);
      setCorrelationCategoryId('');
      setCorrelationData(null);
      return;
    }

    let cancelled = false;

    const loadLocalCategories = async () => {
      try {
        const categories = [];
        let currentPage = 0;
        let totalPagesToFetch = 1;

        while (currentPage < totalPagesToFetch) {
          const data = await getCategories(currentPage, 100);
          categories.push(...(data.categories || []));
          totalPagesToFetch = data.totalPages || 0;
          currentPage += 1;
        }

        if (cancelled) return;

        const options = categories.filter((option) => (
          option.categoryId !== categoryId && !isGlobalCategory(option)
        ));

        setLocalCategoryOptions(options);
        setCorrelationCategoryId((current) => (
          options.some((option) => option.categoryId === current)
            ? current
            : options[0]?.categoryId || ''
        ));
      } catch (err) {
        if (cancelled) return;
        setLocalCategoryOptions([]);
        setCorrelationCategoryId('');
        setCorrelationError(err.message || 'Failed to load local categories');
      }
    };

    loadLocalCategories();

    return () => {
      cancelled = true;
    };
  }, [category, categoryId]);

  useEffect(() => {
    if (!category || !currentUser?.username || !isGlobalCategory(category)) return;

    let cancelled = false;

    getUserCategoryTopScore(currentUser.username, categoryId)
      .then((score) => {
        if (cancelled) return;
        const savedScore = score?.score == null ? NaN : Number(score.score);
        if (Number.isFinite(savedScore)) {
          setLatestSubmittedScore(savedScore);
        }
      })
      .catch(() => {
        // No saved score for this global category yet.
      });

    return () => {
      cancelled = true;
    };
  }, [category, categoryId, currentUser?.username]);

  useEffect(() => {
    if (!category) return;

    setLoading(true);
    const loadPageData = isGlobalCategory(category)
      ? Promise.all([loadScores(0), loadGlobalDataset()])
      : Promise.all([loadScores(0), loadBaselines(), loadChartScores()]);

    loadPageData.finally(() => setLoading(false));
  }, [category, loadScores, loadBaselines, loadChartScores, loadGlobalDataset, filterRequestId]);

  useEffect(() => {
    loadCorrelation();
  }, [loadCorrelation]);

  const handleFilter = () => {
    setPage(0);
    setAppliedFilterRegion(filterRegion);
    setAppliedFilterSex(filterSex);
    setFilterRequestId((current) => current + 1);
  };

  const clearFilters = () => {
    setFilterRegion('');
    setFilterSex('');
    setAppliedFilterRegion('');
    setAppliedFilterSex('');
    setPage(0);
    setFilterRequestId((current) => current + 1);
  };

  const handleGlobalFilter = () => {
    setAppliedGlobalFilterSex(globalFilterSex);
    setAppliedGlobalFilterAgeGroup(globalFilterAgeGroup);
    setAppliedGlobalFilterRegion(globalFilterRegion);
    setFilterRequestId((current) => current + 1);
  };

  const clearGlobalFilters = () => {
    setGlobalFilterSex('');
    setGlobalFilterAgeGroup('');
    setGlobalFilterRegion('');
    setAppliedGlobalFilterSex('');
    setAppliedGlobalFilterAgeGroup('');
    setAppliedGlobalFilterRegion('');
    setFilterRequestId((current) => current + 1);
  };

  const handleScoreChange = (e) => {
    const val = e.target.value;
    if (val === '' || val === '-' || val === '.') {
      setScoreValue(val);
      return;
    }
    if (/e/i.test(val)) return;
    setScoreValue(val);
  };

  const handleSubmitScore = async (e) => {
    e.preventDefault();
    setSubmitError('');

    const minLimit = category.lowerLimit ?? MIN_SCORE;
    const maxLimit = category.upperLimit ?? MAX_SCORE;

    if (!isValidScore(scoreValue, minLimit, maxLimit)) {
      setSubmitError(`Score must be a plain number between ${minLimit} and ${maxLimit.toLocaleString()}. Scientific notation is not allowed.`);
      return;
    }

    const num = parseFloat(scoreValue);
    setSubmitting(true);
    try {
      const tags = {};
      if (currentUser.demographics) {
        Object.assign(tags, currentUser.demographics);
      }

      if (isGlobalCategory(category)) {
        await submitScore({
          user_id: currentUser.userId,
          category_id: categoryId,
          score: num,
          tags,
          anonymous: false,
        });
        const comparison = await compareGlobalStat(categoryId, { score: num, tags }, getGlobalFilterTags());
        setGlobalComparison(comparison);
        setGlobalDataset({
          ...(globalDataset || {}),
          categoryId: comparison.categoryId,
          categoryName: comparison.categoryName,
          units: comparison.units,
          sourceName: comparison.sourceName,
          sourceUrl: comparison.sourceUrl,
          mean: comparison.mean,
          standardDeviation: comparison.standardDeviation,
          sampleSize: comparison.sampleSize,
          histogram: comparison.histogram,
          scatterPoints: comparison.scatterPoints,
        });
        setScoreValue('');
        setLatestSubmittedScore(num);
        await loadScores(page);
        return;
      }

      await submitScore({
        user_id: currentUser.userId,
        category_id: categoryId,
        score: num,
        tags,
        anonymous,
      });
      setScoreValue('');
      setLatestSubmittedScore(num);
      await Promise.all([loadScores(page), loadBaselines(), loadChartScores(), loadCorrelation()]);
    } catch (err) {
      setSubmitError(err.message || 'Failed to submit score');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <div className="page"><div className="loading">Loading...</div></div>;
  if (!category) return <div className="page"><div className="error-banner">{error || 'Category not found'}</div></div>;

  const baseline = baselines.length > 0 ? baselines[0] : null;
  const categoryImage = getCategoryImage(category);
  const globalCategory = isGlobalCategory(category);

  const getRankLabel = (rank) => {
    if (rank === 1) return '1st';
    if (rank === 2) return '2nd';
    if (rank === 3) return '3rd';
    return `${rank}th`;
  };

  const getMedal = (rank) => {
    if (rank === 1) return '\u{1F947}';
    if (rank === 2) return '\u{1F948}';
    if (rank === 3) return '\u{1F949}';
    return '';
  };

const formatNumber = (num) => {
    if (num == null) return '-';
    const n = Number(num);
    if (Math.abs(n) >= 1e8) {
      return n.toExponential(3);
    }
    
    return n.toLocaleString(undefined, { maximumFractionDigits: 2 });
  };
  const formatCompactNumber = (num) => {
    if (num == null) return '-';
    return Number(num).toLocaleString(undefined, {
      notation: 'compact',
      maximumFractionDigits: 1,
    });
  };

  const localChartValues = chartScores
    .map((entry) => Number(entry.score))
    .filter((score) => Number.isFinite(score));
  const globalChartValues = (globalDataset?.values || [])
    .map((value) => Number(value))
    .filter((value) => Number.isFinite(value));
  const globalHistogramBins = (globalDataset?.histogram || [])
    .map((bin) => ({
      start: Number(bin.start),
      end: Number(bin.end),
      count: Number(bin.count),
    }))
    .filter((bin) => Number.isFinite(bin.start) && Number.isFinite(bin.end) && Number.isFinite(bin.count));
  const histogramBins = globalCategory
    ? globalChartValues.length > 0
      ? buildHistogram(globalChartValues)
      : globalHistogramBins
    : buildHistogram(localChartValues);
  const hasChartData = histogramBins.length > 0;
  const chartValues = globalCategory
    ? globalChartValues.length > 0
      ? globalChartValues
      : histogramBins.flatMap((bin) => [bin.start, bin.end])
    : localChartValues;
  const chartMin = hasChartData ? Math.min(...histogramBins.map((bin) => bin.start)) : 0;
  const chartMax = hasChartData ? Math.max(...histogramBins.map((bin) => bin.end)) : 0;
  const xAxisTicks = hasChartData
    ? chartMin === chartMax
      ? [chartMin]
      : [histogramBins[0].start, ...histogramBins.map((bin) => bin.end)]
    : [];
  const maxBucketCount = histogramBins.reduce((max, bin) => Math.max(max, bin.count), 0);
  const currentUserChartEntry = currentUser
    ? chartScores.find((entry) => !entry.anonymous && entry.username === currentUser.username)
    : null;
  const userScoreValue = globalCategory
    ? globalComparison?.submittedValue ?? latestSubmittedScore
    : currentUserChartEntry
      ? Number(currentUserChartEntry.score)
      : latestSubmittedScore;
  const numericUserScoreValue = userScoreValue == null ? NaN : Number(userScoreValue);
  const hasUserMarker = Number.isFinite(numericUserScoreValue) && hasChartData;
  const chartLeft = 48;
  const chartRight = 620;
  const chartTop = 48;
  const chartBaseY = 220;
  const chartHeight = chartBaseY - chartTop;
  const chartWidth = chartRight - chartLeft;
  const markerRatio = hasUserMarker
    ? chartMin === chartMax
      ? 0.5
      : (numericUserScoreValue - chartMin) / (chartMax - chartMin)
    : 0;
  const markerX = chartLeft + clamp(markerRatio, 0, 1) * chartWidth;
  const markerLabelX = clamp(markerX, 72, 596);
  const markerTextAnchor = markerX < 72 ? 'start' : markerX > 596 ? 'end' : 'middle';
  const chartCountLabel = globalCategory
    ? `${formatNumber(globalChartValues.length || globalDataset?.sampleSize || chartTotalElements)} reference points`
    : chartTotalElements > chartScores.length
    ? `${chartScores.length} / ${chartTotalElements} participants`
    : `${chartScores.length} participants`;
  const percentilePoints = buildPercentilePoints(chartValues);
  const hasPercentileData = percentilePoints.length > 0;
  const percentileMin = hasPercentileData ? Math.min(...percentilePoints.map((point) => point.value)) : 0;
  const percentileMax = hasPercentileData ? Math.max(...percentilePoints.map((point) => point.value)) : 0;
  const percentilePath = hasPercentileData
    ? percentilePoints.map((point, index) => {
      const x = chartLeft + (point.percentile / 100) * chartWidth;
      const y = percentileMin === percentileMax
        ? chartTop + chartHeight / 2
        : chartBaseY - ((point.value - percentileMin) / (percentileMax - percentileMin)) * chartHeight;
      return `${index === 0 ? 'M' : 'L'} ${x} ${y}`;
    }).join(' ')
    : '';
  const percentileYAxisTicks = hasPercentileData
    ? percentileMin === percentileMax
      ? [percentileMin]
      : [percentileMax, (percentileMin + percentileMax) / 2, percentileMin]
    : [];
  const correlationPoints = (correlationData?.points || [])
    .map((point) => ({
      primaryScore: Number(point.primaryScore),
      secondaryScore: Number(point.secondaryScore),
    }))
    .filter((point) => Number.isFinite(point.primaryScore) && Number.isFinite(point.secondaryScore));
  const hasCorrelationData = correlationPoints.length > 0;
  const correlationMinX = hasCorrelationData ? Math.min(...correlationPoints.map((point) => point.primaryScore)) : 0;
  const correlationMaxX = hasCorrelationData ? Math.max(...correlationPoints.map((point) => point.primaryScore)) : 0;
  const correlationMinY = hasCorrelationData ? Math.min(...correlationPoints.map((point) => point.secondaryScore)) : 0;
  const correlationMaxY = hasCorrelationData ? Math.max(...correlationPoints.map((point) => point.secondaryScore)) : 0;
  const correlationLabel = correlationData?.pearsonCorrelation == null
    ? 'r: -'
    : `r: ${Number(correlationData.pearsonCorrelation).toFixed(3)}`;
  const correlationCountLabel = correlationData
    ? `${formatNumber(correlationData.sampleSize)} paired participants`
    : '';
  const filtersActive = Boolean(appliedFilterRegion || appliedFilterSex);
  const globalFiltersActive = Boolean(appliedGlobalFilterSex || appliedGlobalFilterAgeGroup || appliedGlobalFilterRegion);
  const chartFiltersActive = globalCategory ? globalFiltersActive : filtersActive;
  const rankSource = chartScores.length > 0 ? chartScores : scores;
  const rankByScore = buildCompetitionRankByScore(rankSource);
  const pageStartRank = page * 25 + 1;

  const handleAdminDeleteCategory = async () => {
    if (!window.confirm('Are you sure?')) return;
    try {
      await adminDeleteCategory(categoryId);
      sessionStorage.removeItem('categoriesCache');
      navigate('/');
    } catch (err) {
      setAdminError(err.message);
    }
  };

  const handleAdminDeleteScore = async (scoreId) => {
    if (!window.confirm('Are you sure?')) return;
    try {
      await adminDeleteScore(scoreId);
      await Promise.all([loadScores(page), loadBaselines(), loadChartScores(), loadCorrelation()]);
    } catch (err) {
      setAdminError(err.message);
    }
  };

  return (
    <div className="page">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12 }}>
        <div className="category-title-row">
          {categoryImage && (
            <img className="category-title-icon" src={categoryImage} alt={`${category.name} icon`} />
          )}
          <h1 className="page-title" style={{ marginBottom: 0 }}>{category.name}</h1>
        </div>
        {isAdmin && (
          <div style={{ position: 'relative' }}>
            <button className="btn btn-ghost btn-sm" onClick={() => setAdminMenuOpen((o) => !o)}>
              Options &#9662;
            </button>
            {adminMenuOpen && (
              <div className="panel" style={{ position: 'absolute', right: 0, top: '110%', zIndex: 10, padding: 8, minWidth: 200 }}>
                <button
                  className="btn btn-ghost btn-sm btn-full"
                  onClick={() => { setAdminMenuOpen(false); navigate(`/admin/category/${categoryId}`); }}
                >
                  Edit properties
                </button>
                <button
                  className="btn btn-ghost btn-sm btn-full"
                  style={{ color: 'crimson' }}
                  onClick={() => { setAdminMenuOpen(false); handleAdminDeleteCategory(); }}
                >
                  Delete category
                </button>
              </div>
            )}
          </div>
        )}
      </div>
      {adminError && <div className="error-banner" style={{ marginTop: 12 }}>{adminError}</div>}
      {category.description && (
  <p 
    style={{ 
      color: 'var(--text-muted)', 
      marginBottom: 28,
      marginTop: 14,
      display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical',
      overflow: 'hidden', 
      textOverflow: 'ellipsis' 
    }}
  >
    {category.description}
  </p>
)}

      {error && <div className="error-banner">{error}</div>}

      <div className="category-layout">
        <div className="leaderboard-section">
          <div className="panel" style={{ padding: 0, overflow: 'hidden' }}>
            <div style={{ padding: '16px 24px', borderBottom: '1px solid var(--border)' }}>
              <div className="panel-title" style={{ marginBottom: 0 }}>
                Leaderboard
                {!globalCategory && filtersActive && <span className="tag" style={{ marginLeft: 8 }}>Filtered</span>}
              </div>
            </div>

            <div style={{ maxHeight: 600, overflowY: 'auto' }}>
              {scores.length === 0 ? (
                <div className="empty-state"><p>No scores submitted yet.</p></div>
              ) : (
                <table className="leaderboard-table">
                  <thead>
                    <tr>
                      <th>Rank</th>
                      <th>Player</th>
                      <th>Score ({category.units})</th>
                      <th>Date</th>
                      {isAdmin && <th></th>}
                    </tr>
                  </thead>
                  <tbody>
                    {scores.map((entry, idx) => {
                      const score = Number(entry.score);
                      const rank = rankByScore.get(score) ?? pageStartRank + idx;

                      return (
                        <tr key={entry.scoreId}>
                          <td className="rank-cell">
                            <span className="rank-medal">{getMedal(rank)}</span>{' '}
                            {getRankLabel(rank)}
                          </td>
                          <td className={`name-cell ${entry.anonymous ? 'anonymous-name' : ''}`}>
                            {entry.username}
                          </td>
                          <td className="score-cell">{formatNumber(entry.score)}</td>
                          <td style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                            {entry.submittedAt ? new Date(entry.submittedAt).toLocaleDateString() : '-'}
                          </td>
                          {isAdmin && (
                            <td>
                              <button
                                className="btn btn-ghost btn-sm"
                                style={{ color: 'crimson' }}
                                onClick={() => handleAdminDeleteScore(entry.scoreId)}
                              >
                                Delete
                              </button>
                            </td>
                          )}
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              )}
            </div>

            {totalPages > 1 && (
              <div className="pagination" style={{ padding: 12, borderTop: '1px solid var(--border)' }}>
                <button className="btn btn-ghost btn-sm" disabled={page === 0} onClick={() => { setPage(page - 1); loadScores(page - 1); }}>Prev</button>
                <span className="page-info">Page {page + 1} of {totalPages}</span>
                <button className="btn btn-ghost btn-sm" disabled={page >= totalPages - 1} onClick={() => { setPage(page + 1); loadScores(page + 1); }}>Next</button>
              </div>
            )}
          </div>

          <div className="panel score-distribution-panel">
            <div className="chart-panel-header">
              <div>
                <div className="panel-title">
                  Score Distribution
                  {chartFiltersActive && <span className="tag" style={{ marginLeft: 8 }}>Filtered</span>}
                </div>
                <div className="chart-subtitle">{globalCategory ? 'Reference population' : 'Best score per user'}</div>
              </div>
              {hasChartData && <div className="chart-count">{chartCountLabel}</div>}
            </div>

            {chartError ? (
              <div className="error-banner">{chartError}</div>
            ) : chartLoading ? (
              <div className="chart-loading">Loading...</div>
            ) : !hasChartData ? (
              <div className="empty-state"><p>No scores to visualize yet.</p></div>
            ) : (
              <div className="chart-shell">
                <svg className="score-chart" viewBox="0 0 640 280" role="img" aria-label={`${category.name} score distribution`}>
                  <title>{`${category.name} score distribution`}</title>
                  <line className="chart-grid-line" x1={chartLeft} y1={chartTop} x2={chartRight} y2={chartTop} />
                  <line className="chart-grid-line" x1={chartLeft} y1={chartTop + chartHeight / 2} x2={chartRight} y2={chartTop + chartHeight / 2} />
                  <line className="chart-axis" x1={chartLeft} y1={chartBaseY} x2={chartRight} y2={chartBaseY} />

                  {histogramBins.map((bin, index) => {
                    const slotWidth = chartWidth / histogramBins.length;
                    const barHeight = maxBucketCount > 0
                      ? Math.max((bin.count / maxBucketCount) * chartHeight, bin.count > 0 ? 4 : 0)
                      : 0;
                    const barWidth = Math.max(slotWidth - 5, 4);
                    const x = chartLeft + index * slotWidth + 2.5;
                    const y = chartBaseY - barHeight;

                    return (
                      <rect
                        key={`${bin.start}-${bin.end}`}
                        className="chart-bar"
                        x={x}
                        y={y}
                        width={barWidth}
                        height={barHeight}
                        rx="3"
                      >
                        <title>{`${formatNumber(bin.start)} - ${formatNumber(bin.end)}: ${bin.count}`}</title>
                      </rect>
                    );
                  })}

                  {hasUserMarker && (
                    <g className="you-marker">
                      <line className="you-marker-line" x1={markerX} y1="34" x2={markerX} y2={chartBaseY} />
                      <circle className="you-marker-dot" cx={markerX} cy={chartBaseY} r="4" />
                      <text className="you-marker-label" x={markerLabelX} y="24" textAnchor={markerTextAnchor}>
                        This is you
                      </text>
                    </g>
                  )}

                  {xAxisTicks.map((tick, index) => {
                    const x = xAxisTicks.length === 1
                      ? chartLeft + chartWidth / 2
                      : chartLeft + (index / (xAxisTicks.length - 1)) * chartWidth;
                    const textAnchor = index === 0
                      ? 'start'
                      : index === xAxisTicks.length - 1
                        ? 'end'
                        : 'middle';

                    return (
                      <g key={`${tick}-${index}`}>
                        <line className="chart-tick" x1={x} y1={chartBaseY} x2={x} y2={chartBaseY + 5} />
                        <text className="chart-axis-label" x={x} y="248" textAnchor={textAnchor}>
                          {formatCompactNumber(tick)}
                        </text>
                      </g>
                    );
                  })}
                  <text className="chart-unit-label" x={(chartLeft + chartRight) / 2} y="270" textAnchor="middle">
                    {category.units}
                  </text>
                </svg>
              </div>
            )}
          </div>

          <div className="panel score-distribution-panel">
            <div className="chart-panel-header">
              <div>
                <div className="panel-title">
                  Percentile Distribution
                  {chartFiltersActive && <span className="tag" style={{ marginLeft: 8 }}>Filtered</span>}
                </div>
                <div className="chart-subtitle">Score value by percentile</div>
              </div>
              {hasPercentileData && <div className="chart-count">{formatNumber(percentilePoints.length)} points</div>}
            </div>

            {chartError ? (
              <div className="error-banner">{chartError}</div>
            ) : chartLoading ? (
              <div className="chart-loading">Loading...</div>
            ) : !hasPercentileData ? (
              <div className="empty-state"><p>No percentile data to visualize yet.</p></div>
            ) : (
              <div className="chart-shell">
                <svg className="score-chart" viewBox="0 0 640 280" role="img" aria-label={`${category.name} percentile distribution`}>
                  <title>{`${category.name} percentile distribution`}</title>
                  <line className="chart-grid-line" x1={chartLeft} y1={chartTop} x2={chartRight} y2={chartTop} />
                  <line className="chart-grid-line" x1={chartLeft} y1={chartTop + chartHeight / 2} x2={chartRight} y2={chartTop + chartHeight / 2} />
                  <line className="chart-axis" x1={chartLeft} y1={chartBaseY} x2={chartRight} y2={chartBaseY} />
                  <line className="chart-axis" x1={chartLeft} y1={chartTop} x2={chartLeft} y2={chartBaseY} />

                  <path className="chart-line" d={percentilePath} />
                  {percentilePoints.length === 1 && (
                    <circle
                      className="chart-scatter-point"
                      cx={chartLeft + chartWidth / 2}
                      cy={chartTop + chartHeight / 2}
                      r="5"
                    />
                  )}

                  {[0, 25, 50, 75, 100].map((tick, index) => {
                    const x = chartLeft + (tick / 100) * chartWidth;
                    const textAnchor = index === 0 ? 'start' : index === 4 ? 'end' : 'middle';

                    return (
                      <g key={tick}>
                        <line className="chart-tick" x1={x} y1={chartBaseY} x2={x} y2={chartBaseY + 5} />
                        <text className="chart-axis-label" x={x} y="248" textAnchor={textAnchor}>
                          {tick}%
                        </text>
                      </g>
                    );
                  })}

                  {percentileYAxisTicks.map((tick, index) => {
                    const y = percentileYAxisTicks.length === 1
                      ? chartTop + chartHeight / 2
                      : chartTop + (index / (percentileYAxisTicks.length - 1)) * chartHeight;

                    return (
                      <g key={`${tick}-${index}`}>
                        <line className="chart-tick" x1={chartLeft - 5} y1={y} x2={chartLeft} y2={y} />
                        <text className="chart-axis-label" x={chartLeft - 8} y={y + 4} textAnchor="end">
                          {formatCompactNumber(tick)}
                        </text>
                      </g>
                    );
                  })}

                  <text className="chart-unit-label" x={(chartLeft + chartRight) / 2} y="270" textAnchor="middle">
                    Percentile
                  </text>
                </svg>
              </div>
            )}
          </div>

          {!globalCategory && (
            <div className="panel score-distribution-panel">
              <div className="chart-panel-header">
                <div>
                  <div className="panel-title">Correlation Graph</div>
                  <div className="chart-subtitle">Best-score pairs from users who submitted to both categories</div>
                </div>
                <div className="chart-header-controls">
                  <select
                    className="input correlation-select"
                    value={correlationCategoryId}
                    onChange={(e) => setCorrelationCategoryId(e.target.value)}
                    disabled={localCategoryOptions.length === 0}
                  >
                    {localCategoryOptions.length === 0 ? (
                      <option value="">No other local categories</option>
                    ) : (
                      localCategoryOptions.map((option) => (
                        <option key={option.categoryId} value={option.categoryId}>
                          {option.name}
                        </option>
                      ))
                    )}
                  </select>
                  {correlationData && <div className="chart-count">{correlationLabel}</div>}
                  {correlationCountLabel && <div className="chart-count">{correlationCountLabel}</div>}
                </div>
              </div>

              {correlationError ? (
                <div className="error-banner">{correlationError}</div>
              ) : correlationLoading ? (
                <div className="chart-loading">Loading...</div>
              ) : localCategoryOptions.length === 0 ? (
                <div className="empty-state"><p>No other local categories are available for comparison.</p></div>
              ) : !hasCorrelationData ? (
                <div className="empty-state"><p>No paired scores to visualize yet.</p></div>
              ) : (
                <div className="chart-shell">
                  <svg className="score-chart" viewBox="0 0 640 280" role="img" aria-label={`${category.name} correlation graph`}>
                    <title>{`${category.name} correlation graph`}</title>
                    <line className="chart-grid-line" x1={chartLeft} y1={chartTop} x2={chartRight} y2={chartTop} />
                    <line className="chart-grid-line" x1={chartLeft} y1={chartTop + chartHeight / 2} x2={chartRight} y2={chartTop + chartHeight / 2} />
                    <line className="chart-axis" x1={chartLeft} y1={chartBaseY} x2={chartRight} y2={chartBaseY} />
                    <line className="chart-axis" x1={chartLeft} y1={chartTop} x2={chartLeft} y2={chartBaseY} />

                    {correlationPoints.map((point, index) => {
                      const xRatio = correlationMinX === correlationMaxX
                        ? 0.5
                        : (point.primaryScore - correlationMinX) / (correlationMaxX - correlationMinX);
                      const yRatio = correlationMinY === correlationMaxY
                        ? 0.5
                        : (point.secondaryScore - correlationMinY) / (correlationMaxY - correlationMinY);
                      const x = chartLeft + clamp(xRatio, 0, 1) * chartWidth;
                      const y = chartBaseY - clamp(yRatio, 0, 1) * chartHeight;

                      return (
                        <circle
                          key={`${point.primaryScore}-${point.secondaryScore}-${index}`}
                          className="chart-scatter-point"
                          cx={x}
                          cy={y}
                          r="4"
                        >
                          <title>{`${formatNumber(point.primaryScore)} ${category.units}, ${formatNumber(point.secondaryScore)} ${correlationData.secondaryUnits}`}</title>
                        </circle>
                      );
                    })}

                    {[
                      correlationMinX,
                      (correlationMinX + correlationMaxX) / 2,
                      correlationMaxX,
                    ].filter((tick, index, ticks) => correlationMinX !== correlationMaxX || index === 1).map((tick, index, ticks) => {
                      const x = ticks.length === 1
                        ? chartLeft + chartWidth / 2
                        : chartLeft + (index / (ticks.length - 1)) * chartWidth;
                      const textAnchor = index === 0 ? 'start' : index === ticks.length - 1 ? 'end' : 'middle';

                      return (
                        <g key={`${tick}-${index}`}>
                          <line className="chart-tick" x1={x} y1={chartBaseY} x2={x} y2={chartBaseY + 5} />
                          <text className="chart-axis-label" x={x} y="248" textAnchor={textAnchor}>
                            {formatCompactNumber(tick)}
                          </text>
                        </g>
                      );
                    })}

                    {[
                      correlationMaxY,
                      (correlationMinY + correlationMaxY) / 2,
                      correlationMinY,
                    ].filter((tick, index) => correlationMinY !== correlationMaxY || index === 1).map((tick, index, ticks) => {
                      const y = ticks.length === 1
                        ? chartTop + chartHeight / 2
                        : chartTop + (index / (ticks.length - 1)) * chartHeight;

                      return (
                        <g key={`${tick}-${index}`}>
                          <line className="chart-tick" x1={chartLeft - 5} y1={y} x2={chartLeft} y2={y} />
                          <text className="chart-axis-label" x={chartLeft - 8} y={y + 4} textAnchor="end">
                            {formatCompactNumber(tick)}
                          </text>
                        </g>
                      );
                    })}

                    <text className="chart-unit-label" x={(chartLeft + chartRight) / 2} y="270" textAnchor="middle">
                      {category.units}
                    </text>
                    <text
                      className="chart-unit-label"
                      x="18"
                      y={(chartTop + chartBaseY) / 2}
                      textAnchor="middle"
                      transform={`rotate(-90 18 ${(chartTop + chartBaseY) / 2})`}
                    >
                      {correlationData.secondaryUnits}
                    </text>
                  </svg>
                </div>
              )}
            </div>
          )}
        </div>

        <div className="panel">
          <div className="panel-title">Statistics</div>
          {globalCategory && globalDataset ? (
            <div className="stats-grid">
              <div className="stat-item"><div className="stat-value">{formatNumber(globalDataset.mean)}</div><div className="stat-label">Mean</div></div>
              <div className="stat-item"><div className="stat-value">{formatNumber(globalDataset.standardDeviation)}</div><div className="stat-label">Std Dev</div></div>
              <div className="stat-item"><div className="stat-value">{formatNumber(globalDataset.sampleSize)}</div><div className="stat-label">Participants</div></div>
              <div className="stat-item"><div className="stat-value">{category.units}</div><div className="stat-label">Unit</div></div>
              {globalComparison && (
                <>
                  <div className="stat-item"><div className="stat-value">{formatNumber(globalComparison.percentile)}%</div><div className="stat-label">Percentile</div></div>
                  <div className="stat-item"><div className="stat-value">{formatNumber(globalComparison.rankEstimate)}</div><div className="stat-label">Rank Estimate</div></div>
                </>
              )}
            </div>
          ) : baseline ? (
            <div className="stats-grid">
              <div className="stat-item"><div className="stat-value">{formatNumber(baseline.mean)}</div><div className="stat-label">Mean</div></div>
              <div className="stat-item"><div className="stat-value">{formatNumber(baseline.standardDeviation)}</div><div className="stat-label">Std Dev</div></div>
              <div className="stat-item"><div className="stat-value">{baseline.sampleSize ?? '-'}</div><div className="stat-label">Participants</div></div>
              <div className="stat-item"><div className="stat-value">{totalElements}</div><div className="stat-label">Total Entries</div></div>
              {baseline.median != null && <div className="stat-item"><div className="stat-value">{formatNumber(baseline.median)}</div><div className="stat-label">Median</div></div>}
              <div className="stat-item"><div className="stat-value">{category.units}</div><div className="stat-label">Unit</div></div>
            </div>
          ) : (
            <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>No baseline statistics available yet.</p>
          )}
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
          {!globalCategory && (
            <div className="panel">
              <div className="panel-title">Filters</div>
              <div className="filter-row">
                <div className="filter-group">
                  <span className="filter-label">Region</span>
                  <select className="input" value={filterRegion} onChange={(e) => setFilterRegion(e.target.value)}>
                    <option value="">All Regions</option>
                    <option value="North America">North America</option>
                    <option value="South America">South America</option>
                    <option value="Europe">Europe</option>
                    <option value="Africa">Africa</option>
                    <option value="Asia">Asia</option>
                    <option value="Oceania">Oceania</option>
                  </select>
                </div>
                <div className="filter-group">
                  <span className="filter-label">Sex</span>
                  <select className="input" value={filterSex} onChange={(e) => setFilterSex(e.target.value)}>
                    <option value="">All</option>
                    <option value="Male">Male</option>
                    <option value="Female">Female</option>
                    <option value="Other">Other</option>
                  </select>
                </div>
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                <button className="btn btn-primary btn-sm" onClick={handleFilter}>Apply Filters</button>
                {filtersActive && <button className="btn btn-ghost btn-sm" onClick={clearFilters}>Clear</button>}
              </div>
            </div>
          )}

          {globalCategory && (isNhanesCategory(category) || isHeightCategory(category)) && (
            <div className="panel">
              <div className="panel-title">Reference Filters</div>
              <div className="form-group">
                <label>Sex</label>
                <div className="sort-toggle">
                  {['', 'Male', 'Female'].map((value) => (
                    <button
                      key={value || 'all-sex'}
                      type="button"
                      className={`btn btn-ghost ${globalFilterSex === value ? 'selected' : ''}`}
                      onClick={() => setGlobalFilterSex(value)}
                    >
                      {value || 'All'}
                    </button>
                  ))}
                </div>
              </div>
              {isNhanesCategory(category) && (
                <div className="form-group">
                  <label>Age Group</label>
                  <select
                    className="input"
                    value={globalFilterAgeGroup}
                    onChange={(e) => setGlobalFilterAgeGroup(e.target.value)}
                  >
                    <option value="">All</option>
                    <option value="Under 18">Under 18</option>
                    <option value="18-39">18-39</option>
                    <option value="40-59">40-59</option>
                    <option value="60+">60+</option>
                  </select>
                </div>
              )}
              {isHeightCategory(category) && (
                <div className="form-group">
                  <label>Region</label>
                  <select
                    className="input"
                    value={globalFilterRegion}
                    onChange={(e) => setGlobalFilterRegion(e.target.value)}
                  >
                    <option value="">All Regions</option>
                    <option value="Africa">Africa</option>
                    <option value="Asia">Asia</option>
                    <option value="Europe">Europe</option>
                    <option value="North America">North America</option>
                    <option value="Oceania">Oceania</option>
                    <option value="South America">South America</option>
                  </select>
                </div>
              )}
              <div style={{ display: 'flex', gap: 8 }}>
                <button className="btn btn-primary btn-sm" onClick={handleGlobalFilter}>Apply Filters</button>
                {globalFiltersActive && <button className="btn btn-ghost btn-sm" onClick={clearGlobalFilters}>Clear</button>}
              </div>
            </div>
          )}

          <div className="panel">
            <div className="panel-title">Submit Score</div>
            {currentUser ? (
              <form className="submit-form" onSubmit={handleSubmitScore}>
                {submitError && <div className="error-banner">{submitError}</div>}
                {globalComparison && (
                  <div className="status-banner">
                    You are in the {formatNumber(globalComparison.percentile)} percentile. Estimated rank: {formatNumber(globalComparison.rankEstimate)}.
                  </div>
                )}
                <div className="score-input-row">
                  <input
                    className="input"
                    type="number"
                    step="any"
                    min={category.lowerLimit ?? MIN_SCORE}
                    max={category.upperLimit ?? MAX_SCORE}
                    value={scoreValue}
                    onChange={handleScoreChange}
                    placeholder="0"
                    required
                  />
                  <span className="unit-label">{category.units}</span>
                </div>
                <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', margin: '4px 0 8px' }}>
                  Valid range: {category.lowerLimit ?? MIN_SCORE} – {(category.upperLimit ?? MAX_SCORE).toLocaleString()}
                </p>
                {!globalCategory && (
                  <label style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                    <input type="checkbox" checked={anonymous} onChange={(e) => setAnonymous(e.target.checked)} />
                    Submit anonymously
                  </label>
                )}
                <button className="btn btn-primary btn-full" type="submit" disabled={submitting}>
                  {submitting ? 'Submitting...' : globalCategory ? 'Compare' : 'Submit'}
                </button>
              </form>
            ) : (
              <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>Log in to submit a score.</p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
