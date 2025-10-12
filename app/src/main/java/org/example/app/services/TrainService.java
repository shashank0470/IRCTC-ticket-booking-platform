package org.example.app.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.app.entities.Train;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TrainService {
    private List<Train> trainList;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String TRAIN_DB_PATH = "localDB/trains.json";
    private File trainFile;

    public TrainService() throws IOException {
        loadTrainListFromFile();
    }

    private void loadTrainListFromFile() throws IOException {
        // First, try to get the file from the resources folder
        URL resource = getClass().getClassLoader().getResource(TRAIN_DB_PATH);

        if (resource != null) {
            try {
                // Load from resources (works when running from IDE or JAR)
                trainFile = new File(resource.toURI());
                trainList = objectMapper.readValue(trainFile, new TypeReference<>() {});
            } catch (Exception e) {
                // If we can't get a File from resources, read as stream
                InputStream inputStream = getClass().getClassLoader().getResourceAsStream(TRAIN_DB_PATH);
                if (inputStream != null) {
                    trainList = objectMapper.readValue(inputStream, new TypeReference<>() {});
                    inputStream.close();
                    // For saving later, we'll need to create a writable file
                    trainFile = new File("localDB/trains.json");
                } else {
                    throw new IOException("Cannot find " + TRAIN_DB_PATH);
                }
            }
        } else {
            // Fallback: try to load from local file system (for development)
            trainFile = new File("localDB/trains.json");
            if (trainFile.exists()) {
                trainList = objectMapper.readValue(trainFile, new TypeReference<>() {});
            } else {
                // Create the file if it doesn't exist
                System.out.println("trains.json not found, creating new file with empty train list");
                trainList = new ArrayList<>();
                trainFile.getParentFile().mkdirs();
                saveTrainListToFile();
            }
        }
    }

    public List<Train> searchTrains(String source, String destination) {
        return trainList.stream()
                .filter(train -> validTrain(train, source, destination))
                .collect(Collectors.toList());
    }

    //the findFirst returns the very first matching trainId found in the trainList
    public void addTrain(Train newTrain) {
        Optional<Train> existingTrain = trainList.stream()
                .filter(train -> train.getTrainId().equalsIgnoreCase(newTrain.getTrainId()))
                .findFirst();

        if (existingTrain.isPresent()) {
            //if a train with the same trainId exists update it instead of adding new one
            updateTrain(newTrain);
        } else {
            trainList.add(newTrain);
            saveTrainListToFile();
        }
    }

    public void updateTrain(Train updatedTrain) {
        // Find the index of the train with the same trainId
        OptionalInt index = IntStream.range(0, trainList.size())
                .filter(i -> trainList.get(i).getTrainId().equalsIgnoreCase(updatedTrain.getTrainId()))
                .findFirst();

        if (index.isPresent()) {
            // If found, replace the existing train with the updated one
            trainList.set(index.getAsInt(), updatedTrain);
            saveTrainListToFile();
        } else {
            // If not found, treat it as adding a new train
            addTrain(updatedTrain);
        }
    }

    private void saveTrainListToFile() {
        try {
            if (trainFile == null) {
                trainFile = new File("localDB/trains.json");
                trainFile.getParentFile().mkdirs();
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(trainFile, trainList);
        } catch (IOException e) {
            System.err.println("Error saving train list: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean validTrain(Train train, String source, String destination) {
        List<String> stationOrder = train.getStations();

        // Case-insensitive search for source and destination
        int sourceIndex = -1;
        int destinationIndex = -1;

        for (int i = 0; i < stationOrder.size(); i++) {
            if (stationOrder.get(i).equalsIgnoreCase(source)) {
                sourceIndex = i;
            }
            if (stationOrder.get(i).equalsIgnoreCase(destination)) {
                destinationIndex = i;
            }
        }

        return sourceIndex != -1 && destinationIndex != -1 && sourceIndex < destinationIndex;
    }
}