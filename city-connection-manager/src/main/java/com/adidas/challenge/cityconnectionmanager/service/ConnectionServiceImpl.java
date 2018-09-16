package com.adidas.challenge.cityconnectionmanager.service;

import com.adidas.challenge.cityconnectionmanager.model.Connection;
import com.adidas.challenge.cityconnectionmanager.repository.ConnectionInstanceRepository;
import com.adidas.challenge.cityconnectionmanager.repository.ConnectionRepository;
import com.adidas.challenge.cityconnectionmanager.repository.model.ConnectionEntity;
import com.adidas.challenge.cityconnectionmanager.repository.model.ConnectionInstanceEntity;
import com.adidas.challenge.cityconnectionmanager.repository.model.ConnectionKey;
import com.adidas.challenge.cityconnectionmanager.service.converter.ConnectionConverter;
import com.adidas.challenge.cityconnectionmanager.service.converter.ConnectionEntityConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class ConnectionServiceImpl implements ConnectionService {

    @Autowired
    ConnectionInstanceRepository connectionInstanceRepository;

    @Autowired
    ConnectionRepository connectionRepository;

    @Autowired
    ConnectionEntityConverter connectionEntityConverter;

    @Autowired
    ConnectionConverter connectionConverter;

    @Override
    @Async
    public void refreshConnection(String city, String destinyCity) {

        double meanTime = calculateConnectionMeanTime(city, destinyCity);

        if(meanTime == Double.MAX_VALUE) {
            //The time of the connection is MAX VALUE
            //The connection has to be deleted
            deleteConnection(city, destinyCity);
        }
        else {
            Connection connection = Connection.builder()
                    .city(city)
                    .destinyCity(destinyCity)
                    .meanTime(meanTime)
                    .build();

            save(connection);
        }
    }

    private void deleteConnection(String city, String destinyCity) {
        Optional<ConnectionEntity> connectionToDelete = connectionRepository.findById(ConnectionKey.builder().city(city).destinyCity(destinyCity).build());
        if(connectionToDelete.isPresent())
            connectionRepository.delete(connectionToDelete.get());
    }

    public double calculateConnectionMeanTime(String city, String destinyCity) {
        List<ConnectionInstanceEntity> connectionInstanceList = connectionInstanceRepository.findByCityAndDestinyCity(city, destinyCity);
        double meanTime = connectionInstanceList.stream()
                .map(ci -> Duration.between(ci.getDepartureTime(), ci.getArrivalTime())
                        .getSeconds())
                .mapToDouble(n -> n)
                .average().orElse(Double.MAX_VALUE);
        return meanTime;
    }

    private Connection save (Connection connection){
        ConnectionEntity connectionEntity = connectionRepository.save(connectionConverter.convert(connection));
        return connectionEntityConverter.convert(connectionEntity);
    }

    @Override
    public List<Connection> getAll() {
        Iterable<ConnectionEntity> allConnectionEntity = connectionRepository.findAll();
        return StreamSupport.stream(allConnectionEntity.spliterator(), false)
                .map(connectionEntity -> connectionEntityConverter.convert(connectionEntity))
                .collect(Collectors.toList());
    }

}
