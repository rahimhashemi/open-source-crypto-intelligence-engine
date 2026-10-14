package com.rahim.rest;

import com.rahim.candle.CandleService;
import com.rahim.core.candle.Candle;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/coins/{market}/candles")
public class CandleController {
    private final CandleService service;

    public CandleController(CandleService service) {
        this.service = service;
    }

    @PostMapping("/sync")
    public CandleService.SyncResult sync(@PathVariable String market,
            @RequestParam(defaultValue = "1hour") String period,
            @RequestParam(defaultValue = "100") int limit) {
        return service.sync(market, period, limit);
    }

    @GetMapping
    public List<Candle> read(@PathVariable String market,
            @RequestParam(defaultValue = "1hour") String period,
            @RequestParam(defaultValue = "100") int limit) {
        return service.read(market, period, limit);
    }
}
