package com.statit.backend.service;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SasXportParser
{
    public List<Map<String, Object>> fetchRows(String url)
    {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "Statit NHANES data seeder/1.0")
                .GET()
                .build();

        try
        {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if(response.statusCode() < 200 || response.statusCode() >= 300)
            {
                throw new IllegalStateException("Failed to fetch XPT file: " + url);
            }
            return parseRows(response.body());
        }
        catch(IOException e)
        {
            throw new IllegalStateException("Failed to fetch XPT file: " + url, e);
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to fetch XPT file: " + url, e);
        }
    }

    public List<Map<String, Object>> parseRows(byte[] data)
    {
        int namestrHeaderOffset = indexOf(data, NAMESTR_HEADER.getBytes(StandardCharsets.US_ASCII), 0);
        if(namestrHeaderOffset < 0)
        {
            throw new IllegalStateException("XPT file is missing NAMESTR metadata.");
        }

        int variableCount = parseVariableCount(data, namestrHeaderOffset);
        int namestrStart = namestrHeaderOffset + XPT_RECORD_LENGTH;
        List<XportVariable> variables = parseVariables(data, namestrStart, variableCount);

        int metadataEnd = namestrStart + (variableCount * NAMESTR_RECORD_LENGTH);
        int obsHeaderOffset = indexOf(data, OBS_HEADER.getBytes(StandardCharsets.US_ASCII), metadataEnd);
        if(obsHeaderOffset < 0)
        {
            throw new IllegalStateException("XPT file is missing observation data.");
        }

        int rowStart = obsHeaderOffset + XPT_RECORD_LENGTH;
        int rowLength = variables.stream().mapToInt(XportVariable::length).sum();
        List<Map<String, Object>> rows = new ArrayList<>();

        for(int offset = rowStart; offset + rowLength <= data.length; offset += rowLength)
        {
            Map<String, Object> row = parseRow(data, offset, variables);
            if(row.containsKey("SEQN"))
            {
                rows.add(row);
            }
        }

        return rows;
    }

    private int parseVariableCount(byte[] data, int headerOffset)
    {
        String countText = new String(data, headerOffset + 48, 10, StandardCharsets.US_ASCII).trim();
        try
        {
            return Integer.parseInt(countText);
        }
        catch(NumberFormatException e)
        {
            throw new IllegalStateException("XPT NAMESTR header has an invalid variable count.", e);
        }
    }

    private List<XportVariable> parseVariables(byte[] data, int namestrStart, int variableCount)
    {
        List<XportVariable> variables = new ArrayList<>();
        for(int i = 0; i < variableCount; i++)
        {
            int offset = namestrStart + (i * NAMESTR_RECORD_LENGTH);
            int type = readUnsignedShort(data, offset);
            int length = readUnsignedShort(data, offset + 4);
            String name = readAscii(data, offset + 8, 8);
            int position = readInt(data, offset + 84);
            variables.add(new XportVariable(name, type, length, position));
        }
        return variables;
    }

    private Map<String, Object> parseRow(byte[] data, int rowOffset, List<XportVariable> variables)
    {
        Map<String, Object> row = new LinkedHashMap<>();
        for(XportVariable variable : variables)
        {
            int cellOffset = rowOffset + variable.position();
            byte[] raw = Arrays.copyOfRange(data, cellOffset, cellOffset + variable.length());
            Object value = variable.type() == NUMERIC_TYPE ? parseNumeric(raw) : readAscii(raw, 0, raw.length);
            if(value != null)
            {
                row.put(variable.name(), value);
            }
        }
        return row;
    }

    private Double parseNumeric(byte[] raw)
    {
        if(raw.length == 0) return null;
        if(isSasMissing(raw)) return null;

        byte[] normalized = new byte[8];
        System.arraycopy(raw, 0, normalized, 0, Math.min(raw.length, normalized.length));

        boolean allZero = true;
        for(byte b : normalized)
        {
            if(b != 0)
            {
                allZero = false;
                break;
            }
        }
        if(allZero) return 0.0;

        int sign = (normalized[0] & 0x80) == 0 ? 1 : -1;
        int exponent = (normalized[0] & 0x7F) - 64;
        long fraction = 0L;
        for(int i = 1; i < normalized.length; i++)
        {
            fraction = (fraction << 8) | (normalized[i] & 0xFFL);
        }

        double mantissa = fraction / Math.pow(2.0, 56);
        return sign * mantissa * Math.pow(16.0, exponent);
    }

    private boolean isSasMissing(byte[] raw)
    {
        int marker = raw[0] & 0xFF;
        return marker == '.' || marker == '_';
    }

    private int indexOf(byte[] data, byte[] target, int start)
    {
        for(int i = Math.max(start, 0); i <= data.length - target.length; i++)
        {
            boolean match = true;
            for(int j = 0; j < target.length; j++)
            {
                if(data[i + j] != target[j])
                {
                    match = false;
                    break;
                }
            }
            if(match) return i;
        }
        return -1;
    }

    private int readUnsignedShort(byte[] data, int offset)
    {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    private int readInt(byte[] data, int offset)
    {
        return ((data[offset] & 0xFF) << 24)
                | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }

    private String readAscii(byte[] data, int offset, int length)
    {
        return new String(data, offset, length, StandardCharsets.US_ASCII).trim();
    }

    private record XportVariable(String name, int type, int length, int position) {}

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static final String NAMESTR_HEADER = "HEADER RECORD*******NAMESTR HEADER RECORD!!!!!!!";
    private static final String OBS_HEADER = "HEADER RECORD*******OBS     HEADER RECORD!!!!!!!";
    private static final int XPT_RECORD_LENGTH = 80;
    private static final int NAMESTR_RECORD_LENGTH = 140;
    private static final int NUMERIC_TYPE = 1;
}
