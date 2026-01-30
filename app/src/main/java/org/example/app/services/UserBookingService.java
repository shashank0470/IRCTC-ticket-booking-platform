package org.example.app.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.app.entities.Train;
import org.example.app.entities.User;
import org.example.app.utils.UserServiceUtil;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.example.app.entities.Ticket;

public class UserBookingService {
    private User user;
    private List<User> userList;
    private static final String USER_PATH = "localDB/users.json";
    //ObjectMapper in Java is used to convert between Java objects and JSON (both ways).
    //-->Serialization → Java Object ➝ JSON
    //-->Deserialization → JSON ➝ Java Object
    private ObjectMapper objectMapper = new ObjectMapper();
    //File does NOT read or write data
    //It only:
    //Points to a file/directory
    //Checks properties (exists, size, name, path)
    //Creates or deletes files/folders
    private File userFile;

    // Constructor after login: loads file and sets this.user to the actual user from DB (with correct userId and ticketsBooked)
    public UserBookingService(User loginCredentials) throws IOException {
        if (loginCredentials == null || loginCredentials.getName() == null || loginCredentials.getName().trim().isEmpty()) {
            throw new IOException("Invalid login: username is required.");
        }
        if (loginCredentials.getPassword() == null) {
            throw new IOException("Invalid login: password is required.");
        }
        loadUserListFromFile();
        if (userList == null) {
            throw new IOException("Failed to load user data.");
        }
        String name = loginCredentials.getName().trim();
        String password = loginCredentials.getPassword();
        Optional<User> found = userList.stream()
                .filter(u -> u != null && u.getName() != null && u.getName().equalsIgnoreCase(name)
                        && UserServiceUtil.checkPassword(password, u.getHashedPassword()))
                .findFirst();
        if (found.isPresent()) {
            this.user = found.get();
        } else {
            throw new IOException("Invalid username or password.");
        }
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
        if (user == null || userList == null || user.getPassword() == null || user.getName() == null) {
            return false;
        }
        Optional<User> foundUser = userList.stream()
                .filter(u -> u != null && u.getName() != null && u.getName().equalsIgnoreCase(user.getName())
                        && UserServiceUtil.checkPassword(user.getPassword(), u.getHashedPassword()))
                .findFirst();
        return foundUser.isPresent();
    }

    public Boolean signUp(User user1) {
        if (user1 == null || user1.getName() == null || user1.getName().trim().isEmpty()) {
            System.out.println("Invalid user or username.");
            return Boolean.FALSE;
        }
        if (userList == null) {
            System.out.println("Error: user data not loaded. Please try again.");
            return Boolean.FALSE;
        }
        boolean duplicate = userList.stream()
                .anyMatch(u -> u != null && u.getName() != null && u.getName().equalsIgnoreCase(user1.getName().trim()));
        if (duplicate) {
            System.out.println("Username already exists. Please choose a different name.");
            return Boolean.FALSE;
        }
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
        if (userList == null) {
            throw new IOException("Cannot save: user list is not loaded.");
        }
        if (userFile == null) {
            userFile = new File("localDB/users.json");
            userFile.getParentFile().mkdirs();
        }
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(userFile, userList);
    }

    public boolean isLoggedIn() {
        return user != null;
    }

    public void fetchBookings() {
        if (user != null && user.getTicketsBooked() != null && !user.getTicketsBooked().isEmpty()) {
            user.printTickets();
        } else {
            System.out.println("No bookings found.");
        }
    }

    public Boolean cancelBooking(String ticketId) {
        if (ticketId == null || ticketId.trim().isEmpty()) {
            System.out.println("Ticket ID cannot be null or empty.");
            return Boolean.FALSE;
        }
        final String ticketIdToCancel = ticketId.trim();
        if (user == null || user.getTicketsBooked() == null) {
            System.out.println("You must be logged in to cancel a booking.");
            return Boolean.FALSE;
        }

        Optional<Ticket> toCancel = user.getTicketsBooked().stream()
                .filter(t -> ticketIdToCancel.equals(t.getTicketId()))
                .findFirst();

        if (toCancel.isEmpty()) {
            System.out.println("No ticket found with ID " + ticketIdToCancel);
            return Boolean.FALSE;
        }

        Ticket ticket = toCancel.get();
        user.getTicketsBooked().remove(ticket);

        // Free the seat on the train
        Train train = ticket.getTrain();
        if (train != null && train.getSeats() != null) {
            int r = ticket.getSeatRow();
            int c = ticket.getSeatColumn();
            List<List<Integer>> seats = train.getSeats();
            if (r >= 0 && r < seats.size() && c >= 0 && c < seats.get(r).size()) {
                seats.get(r).set(c, 0);
                train.setSeats(seats);
                try {
                    TrainService trainService = new TrainService();
                    trainService.updateTrain(train);
                } catch (IOException e) {
                    System.out.println("Error freeing seat on train; ticket removed from your list.");
                }
            }
        }

        updateCurrentUserInListAndSave();
        System.out.println("Ticket with ID " + ticketIdToCancel + " has been canceled.");
        return Boolean.TRUE;
    }

    private void updateCurrentUserInListAndSave() {
        if (user == null || userList == null) return;
        String uid = user.getUserId();
        for (int i = 0; i < userList.size(); i++) {
            User listUser = userList.get(i);
            if (listUser != null && Objects.equals(uid, listUser.getUserId())) {
                userList.set(i, user);
                break;
            }
        }
        try {
            saveUserListToFile();
        } catch (IOException e) {
            System.out.println("Error saving after update: " + e.getMessage());
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
        if (train == null || train.getSeats() == null) {
            return Collections.emptyList();
        }
        return train.getSeats();
    }

    public Boolean bookTrainSeat(Train train, int row, int seat, String source, String destination, String dateOfTravel) {
        if (user == null) {
            System.out.println("You must be logged in to book a seat.");
            return false;
        }
        if (train == null || train.getSeats() == null) {
            System.out.println("Invalid train or train has no seats.");
            return false;
        }
        if (source == null || (source = source.trim()).isEmpty() || destination == null || (destination = destination.trim()).isEmpty()
                || dateOfTravel == null || (dateOfTravel = dateOfTravel.trim()).isEmpty()) {
            System.out.println("Source, destination and date of travel are required.");
            return false;
        }
        try {
            TrainService trainService = new TrainService();
            List<List<Integer>> seats = train.getSeats();
            if (row < 0 || row >= seats.size() || seat < 0 || seat >= seats.get(row).size()) {
                System.out.println("Invalid row or seat index.");
                return false;
            }
            if (seats.get(row).get(seat) != 0) {
                System.out.println("Seat is already booked.");
                return false;
            }
            seats.get(row).set(seat, 1);
            train.setSeats(seats);
            trainService.updateTrain(train);

            String ticketId = UUID.randomUUID().toString();
            Ticket ticket = new Ticket(ticketId, user.getUserId(), source, destination, dateOfTravel, train, row, seat);
            if (user.getTicketsBooked() == null) {
                user.setTicketsBooked(new ArrayList<>());
            }
            user.getTicketsBooked().add(ticket);
            updateCurrentUserInListAndSave();
            return true;
        } catch (IOException ex) {
            System.out.println("Error booking seat: " + ex.getMessage());
            return Boolean.FALSE;
        }
    }
}