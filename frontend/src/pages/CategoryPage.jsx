import { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { getCategory, getTopScores, getFilteredScores, getBaselines, submitScore } from '../api';

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
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [filterRegion, setFilterRegion] = useState('');
  const [filterSex, setFilterSex] = useState('');
  const [appliedFilterRegion, setAppliedFilterRegion] = useState('');
  const [appliedFilterSex, setAppliedFilterSex] = useState('');
  const [filterRequestId, setFilterRequestId] = useState(0);

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

  const loadBaselines = useCallback(async () => {
    try {
      const data = await getBaselines(categoryId);
      setBaselines(data || []);
    } catch {
      // baselines are optional
    }
  }, [categoryId]);

  useEffect(() => {
    setLoading(true);
    Promise.all([loadCategory(), loadScores(0), loadBaselines(), loadChartScores()]).finally(() =>
      setLoading(false)
    );
  }, [loadCategory, loadScores, loadBaselines, loadChartScores, filterRequestId]);

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

      await submitScore({
        user_id: currentUser.userId,
        category_id: categoryId,
        score: num,
        tags,
        anonymous,
      });
      setScoreValue('');
      setLatestSubmittedScore(num);
      await Promise.all([loadScores(page), loadBaselines(), loadChartScores()]);
    } catch (err) {
      setSubmitError(err.message || 'Failed to submit score');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <div className="page"><div className="loading">Loading...</div></div>;
  if (!category) return <div className="page"><div className="error-banner">{error || 'Category not found'}</div></div>;

  const baseline = baselines.length > 0 ? baselines[0] : null;

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

  const chartValues = chartScores
    .map((entry) => Number(entry.score))
    .filter((score) => Number.isFinite(score));
  const histogramBins = buildHistogram(chartValues);
  const chartMin = chartValues.length > 0 ? Math.min(...chartValues) : 0;
  const chartMax = chartValues.length > 0 ? Math.max(...chartValues) : 0;
  const xAxisTicks = histogramBins.length > 0
    ? chartMin === chartMax
      ? [chartMin]
      : [histogramBins[0].start, ...histogramBins.map((bin) => bin.end)]
    : [];
  const maxBucketCount = histogramBins.reduce((max, bin) => Math.max(max, bin.count), 0);
  const currentUserChartEntry = currentUser
    ? chartScores.find((entry) => !entry.anonymous && entry.username === currentUser.username)
    : null;
  const userScoreValue = currentUserChartEntry ? Number(currentUserChartEntry.score) : latestSubmittedScore;
  const hasUserMarker = Number.isFinite(userScoreValue) && chartValues.length > 0;
  const chartLeft = 48;
  const chartRight = 620;
  const chartTop = 48;
  const chartBaseY = 220;
  const chartHeight = chartBaseY - chartTop;
  const chartWidth = chartRight - chartLeft;
  const markerRatio = hasUserMarker
    ? chartMin === chartMax
      ? 0.5
      : (userScoreValue - chartMin) / (chartMax - chartMin)
    : 0;
  const markerX = chartLeft + clamp(markerRatio, 0, 1) * chartWidth;
  const markerLabelX = clamp(markerX, 72, 596);
  const markerTextAnchor = markerX < 72 ? 'start' : markerX > 596 ? 'end' : 'middle';
  const chartCountLabel = chartTotalElements > chartScores.length
    ? `${chartScores.length} / ${chartTotalElements} participants`
    : `${chartScores.length} participants`;
  const filtersActive = Boolean(appliedFilterRegion || appliedFilterSex);
  const rankSource = chartScores.length > 0 ? chartScores : scores;
  const rankByScore = buildCompetitionRankByScore(rankSource);
  const pageStartRank = page * 25 + 1;

  return (
    <div className="page">
      <h1 className="page-title">{category.name}</h1>
      {category.description && (
  <p 
    style={{ 
      color: 'var(--text-muted)', 
      marginBottom: 24, 
      marginTop: -16,
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
                {filtersActive && <span className="tag" style={{ marginLeft: 8 }}>Filtered</span>}
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
                  {filtersActive && <span className="tag" style={{ marginLeft: 8 }}>Filtered</span>}
                </div>
                <div className="chart-subtitle">Best score per user</div>
              </div>
              {chartValues.length > 0 && <div className="chart-count">{chartCountLabel}</div>}
            </div>

            {chartError ? (
              <div className="error-banner">{chartError}</div>
            ) : chartLoading ? (
              <div className="chart-loading">Loading...</div>
            ) : chartValues.length === 0 ? (
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
        </div>

        <div className="panel">
          <div className="panel-title">Statistics</div>
          {baseline ? (
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

          <div className="panel">
            <div className="panel-title">Submit Score</div>
            {currentUser ? (
              <form className="submit-form" onSubmit={handleSubmitScore}>
                {submitError && <div className="error-banner">{submitError}</div>}
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
                <label style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                  <input type="checkbox" checked={anonymous} onChange={(e) => setAnonymous(e.target.checked)} />
                  Submit anonymously
                </label>
                <button className="btn btn-primary btn-full" type="submit" disabled={submitting}>
                  {submitting ? 'Submitting...' : 'Submit'}
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
