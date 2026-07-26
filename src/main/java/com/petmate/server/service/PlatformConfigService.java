package com.petmate.server.service;

import com.petmate.server.entity.PlatformConfig;
import com.petmate.server.repository.PlatformConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlatformConfigService {

    private final PlatformConfigRepository repository;

    public String getValue(String key) {
        return repository.findById(key)
                .map(PlatformConfig::getConfigValue)
                .orElse(null);
    }

    public int getIntValue(String key, int defaultValue) {
        String val = getValue(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void setValue(String key, String value, String description) {
        PlatformConfig config = repository.findById(key)
                .orElseGet(() -> {
                    PlatformConfig newConfig = new PlatformConfig();
                    newConfig.setConfigKey(key);
                    return newConfig;
                });
        config.setConfigValue(value);
        if (description != null) {
            config.setDescription(description);
        }
        repository.save(config);
    }
}
