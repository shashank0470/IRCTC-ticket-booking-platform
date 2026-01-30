package org.example.app.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.example.app.entities.Train;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Ticket{

    private String ticketId;

    private String userId;

    private String source;

    private String destination;

    private String dateOfTravel;

    private Train train;

    private int seatRow;
    private int seatColumn;

    public Ticket(){};

    //constructor setting up the value
    public Ticket(String ticketId, String userId, String source, String destination, String dateOfTravel, Train train, int seatRow, int seatColumn){
        this.ticketId = ticketId;
        this.userId = userId;
        this.source = source;
        this.destination = destination;
        this.dateOfTravel = dateOfTravel;
        this.train = train;
        this.seatRow = seatRow;
        this.seatColumn = seatColumn;
    }

    public int getSeatRow() { return seatRow; }
    public void setSeatRow(int seatRow) { this.seatRow = seatRow; }
    public int getSeatColumn() { return seatColumn; }
    public void setSeatColumn(int seatColumn) { this.seatColumn = seatColumn; }

    public String getTicketInfo(){
        //String formate is used to crate a formatted String
        return String.format("Ticket ID: %s belongs to User %s from %s to %s on %s", ticketId, userId, source, destination, dateOfTravel);
    }

    public String getTicketId(){
        return ticketId;
    }

    public void setTicketId(String ticketId){
        this.ticketId = ticketId;
    }

    public String getSource(){
        return source;
    }

    public void setSource(String source){
        this.source = source;
    }

    public String getUserId(){
        return userId;
    }

    public void setUserId(String userId){
        this.userId = userId;
    }

    public String getDestination(){
        return destination;
    }

    public void setDestination(String destination){
        this.destination = destination;
    }

    public String getDateOfTravel(){
        return dateOfTravel;
    }

    public void setDateOfTravel(String dateOfTravel){
        this.dateOfTravel = dateOfTravel;
    }

    public Train getTrain(){
        return train;
    }

    public void setTrain(Train train){
        this.train = train;
    }

}