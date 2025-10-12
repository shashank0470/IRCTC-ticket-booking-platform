package org.example.app.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.app.entities.Train;
import org.example.app.entities.User;
import org.example.app.utils.UserServiceUtil;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import org.example.app.entities.Ticket;

public class UserBookingService {
    private User user;
    private List<User> userList;
    private static final String USER_PATH = "localDB/users.json";
    private ObjectMapper objectMapper = new ObjectMapper();
    private File userFile;

    //this constructor is after login
    public UserBookingService(User user1) throws IOException {
        this.user = user1;
        loadUserListFromFile();
    }

    //this constructor before login
    public UserBookingService() throws IOException {
        loadUserListFromFile();
    }

    //this method loads all the users list before the user begins signin or login
    public void loadUserListFromFile() throws IOException {
        // First, try to get the file from the resources folder
        URL resource = getClass().getClassLoader().getResource(USER_PATH);

        if (resource != null) {
            try {
                // Load from resources (works when running from IDE or JAR)
                userFile = new File(resource.toURI());
                userList = objectMapper.readValue(userFile, new TypeReference<List<User>>() {});
            } catch (Exception e) {
                // If we can't get a File from resources, read as stream
                InputStream inputStream = getClass().getClassLoader().getResourceAsStream(USER_PATH);
                if (inputStream != null) {
                    userList = objectMapper.readValue(inputStream, new TypeReference<List<User>>() {});
                    inputStream.close();
                    // For saving later, we'll need to create a writable file
                    userFile = new File("localDB/users.json");
                } else {
                    throw new IOException("Cannot find " + USER_PATH);
                }
            }
        } else {
            // Fallback: try to load from local file system (for development)
            userFile = new File("localDB/users.json");
            if (userFile.exists()) {
                userList = objectMapper.readValue(userFile, new TypeReference<List<User>>() {});
            } else {
                // Create the file if it doesn't exist
                System.out.println("users.json not found, creating new file with empty user list");
                userList = new ArrayList<>();
                userFile.getParentFile().mkdirs();
                saveUserListToFile();
            }
        }
    }

    public Boolean loginUser() {
        Optional<User> foundUser = userList.stream().filter(user1 -> {
            return user1.getName().equalsIgnoreCase(user.getName()) &&
                    UserServiceUtil.checkPassword(user.getPassword(), user1.getHashedPassword());
        }).findFirst();
        return foundUser.isPresent();
    }

    public Boolean signUp(User user1) {
        try {
            userList.add(user1);
            saveUserListToFile();
            return Boolean.TRUE;
        } catch (IOException ex) {
            System.out.println("Error during signup: " + ex.getMessage());
            return Boolean.FALSE;
        }
    }

    private void saveUserListToFile() throws IOException {
        if (userFile == null) {
            userFile = new File("localDB/users.json");
            userFile.getParentFile().mkdirs();
        }
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(userFile, userList);
    }

    public void FetchBooking() {
        if (user != null && user.getTicketsBooked() != null && !user.getTicketsBooked().isEmpty()) {
            user.printTickets();
        } else {
            System.out.println("No bookings found.");
        }
    }

    public Boolean cancelBooking(String ticketId) {
        Scanner s = new Scanner(System.in);
        System.out.println("Enter the ticket id to cancel");
        ticketId = s.next();

        if (ticketId == null || ticketId.isEmpty()) {
            System.out.println("Ticket ID cannot be null or empty.");
            return Boolean.FALSE;
        }

        String finalTicketId = ticketId;
        boolean removed = user.getTicketsBooked().removeIf(ticket ->
                ticket.getTicketId().equals(finalTicketId));

        if (removed) {
            System.out.println("Ticket with ID " + ticketId + " has been canceled.");
            try {
                saveUserListToFile();
            } catch (IOException e) {
                System.out.println("Error saving after cancellation");
            }
            return Boolean.TRUE;
        } else {
            System.out.println("No ticket found with ID " + ticketId);
            return Boolean.FALSE;
        }
    }

    public List<Train> getTrains(String source, String destination) {
        try {
            TrainService trainService = new TrainService();
            return trainService.searchTrains(source, destination);
        } catch (IOException ex) {
            System.out.println("Error loading trains: " + ex.getMessage());
            return new ArrayList<>();
        }
    }

    public List<List<Integer>> fetchSeats(Train train) {
        return train.getSeats();
    }

    public Boolean bookTrainSeat(Train train, int row, int seat) {
        try {
            TrainService trainService = new TrainService();
            List<List<Integer>> seats = train.getSeats();
            if (row >= 0 && row < seats.size() && seat >= 0 && seat < seats.get(row).size()) {
                if (seats.get(row).get(seat) == 0) {
                    seats.get(row).set(seat, 1);
                    train.setSeats(seats);
                    trainService.addTrain(train);
                    return true;
                } else {
                    System.out.println("Seat is already booked.");
                    return false;
                }
            } else {
                System.out.println("Invalid row or seat index.");
                return false;
            }
        } catch (IOException ex) {
            System.out.println("Error booking seat: " + ex.getMessage());
            return Boolean.FALSE;
        }
    }
}