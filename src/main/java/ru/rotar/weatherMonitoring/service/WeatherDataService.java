package ru.rotar.weatherMonitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import ru.rotar.weatherMonitoring.jsonObject.Root;
import ru.rotar.weatherMonitoring.model.City;
import ru.rotar.weatherMonitoring.model.WeatherData;
import ru.rotar.weatherMonitoring.repository.WeatherDataRepository;


import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@EnableScheduling
@Log
public class WeatherDataService {

    private final WeatherDataRepository weatherDataRepository;
    private final WebClient webClient;

    @Value("${json?key}")
    private String keys;

    @Transactional(readOnly = true)
    public WeatherData weatherInTheCity(City city){
        LocalDateTime date = LocalDateTime.now();
        if (date == null) {
            return weatherDataRepository.findFirstByCityOrderByDateDesc(
                    city.name()).orElse(weatherAdd(city));
        } else {
            return weatherDataRepository.findFirstByCityAndDateBeforeOrderByDateDesc(
                    city.name(), date);
        }
    }

    @Transactional(readOnly = true)
    public WeatherData weatherAdd(@NotNull City city) {

        Root rootWeather = webClient
                .get()
                .uri(city.getUri() + keys)
                .retrieve()
                .bodyToMono(Root.class)
                .block();

           WeatherData weatherData = new WeatherData();
                if(rootWeather != null){
                    if(rootWeather.getMain() != null){
                        weatherData.setTemp(rootWeather.getMain().getTemp());
                } else {
                        weatherData.setTemp(0.0);
                }
                    if(rootWeather.getMain() != null){
                        weatherData.setPressure(rootWeather.getMain().getPressure());
                } else {
                        weatherData.setPressure(0);
                    }
                    if(rootWeather.getWind() != null ){
                        weatherData.setSpeed(rootWeather.getWind().getSpeed());
                } else {
                        weatherData.setSpeed(0.0);
                }
                    if(rootWeather.getWeather() != null){
                        weatherData.setMain(rootWeather.getWeather().toString());
                } else {
                        weatherData.setMain(null);
                }
            Objects.requireNonNull(weatherData).setCity(city.name());
            weatherData.setDate(LocalDateTime.now());
        }
        weatherDataRepository.save(weatherData);
        return weatherData;
    }

    @Transactional(readOnly = true)
    @Scheduled(fixedRate = 300000)
    void saveWeather() {
        weatherAdd(City.BREST);
        weatherAdd(City.BEREZA);
        weatherAdd(City.PRUZHANY);
        weatherAdd(City.MINSK);
        weatherAdd(City.GOMEL);
    }
}
