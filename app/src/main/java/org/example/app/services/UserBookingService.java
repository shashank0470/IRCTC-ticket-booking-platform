package org.example.app.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.app.entities.User;

import javax.imageio.IIOException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class UserBookingService {
    private User user;

    //created a list which fetch user from our local database.
    private List<User> userList;

    //
    private static final String USER_PATH = "../app/src/main/java/org/example/app/localDB/ users.json";

    //ObjectMapper is a class in jackson library
    private ObjectMapper objectMapper = new ObjectMapper();


    public  UserBookingService(User user1) throws IOException {
        //this in Java refers to the current object’s field or method, mainly used to resolve naming conflicts or clarify we’re accessing the class’s variable.
        this.user = user1;
        //File is a class which is used to handel the path, it is also used to read, write or delete files
        File users = new File(USER_PATH);
        userList = objectMapper.readValue(users, new TypeReference<List<User>>() {
        });
    }

    public Boolean loginUser(){
        Optional<User> foundUser = userList.stream().filter(user1 -> {
            return user1.getName().equals(user.getName()) && UserServiceUtil.checkPassword(user.getPassword(), user1.getHashedPassword());
        }).findFirst();
        return foundUser.isPresent();
    }

    public Boolean signUp(User user1){
        try{
            userList.add(user1);
            saveUserListToFile();
            return Boolean.TRUE;
        }catch (IOException ex){
            return Boolean.FALSE;
        }
    }
}
