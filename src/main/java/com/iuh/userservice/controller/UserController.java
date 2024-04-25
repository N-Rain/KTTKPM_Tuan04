package com.iuh.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iuh.userservice.models.User;
import com.iuh.userservice.repositories.UserRepository;
import com.iuh.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private UserRepository userRepository;

    private Jedis jedis = new Jedis();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    @PostMapping("/save")
    public User addEmployee(@RequestBody User user) {
        jedis.set(String.valueOf(user.getId()), user.getName());
        System.out.println("saved in cache");
        return userRepository.save(user);
    }

    @GetMapping("/{id}")
//    @Cacheable(value = "users",key = "#userId")
    public User findUserByID(@PathVariable(value = "id") long id) {
        String key = String.valueOf(id);

        // Kiểm tra xem user có trong cache không
        if (jedis.exists(key)) {
            User userInCache = new User();
            userInCache.setId(id);
            System.out.println("fetching from cache: " + id);
            String userName = jedis.get(key);
            userInCache.setName(userName);
            return userInCache;
        } else {
            // Nếu user không có trong cache, lấy từ cơ sở dữ liệu và lưu vào cache
            System.out.println("fetching from database: " + id);
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User_id " + id + " not found"));

            // Lưu user name vào cache
            jedis.set(key, user.getName());
            System.out.println("saved in cache");
            return user;
        }
    }


    @PutMapping("/{id}")
    public User updateUser(@PathVariable(value = "id") long id, @RequestBody User user){
        User userUpdate = userRepository.findById(id).orElseThrow(()-> new RuntimeException("User not found"));
        userUpdate.setName(user.getName());
        jedis.set(String.valueOf(user.getId()), user.getName());
        System.out.println("saved in cache");
        return userRepository.save(userUpdate);
    }

    @DeleteMapping("/{id}")
//    @CacheEvict(value = "users")
    public void deleteUser(@PathVariable(value = "id") long userId){
        User user = userRepository.findById(userId).orElseThrow(()-> new RuntimeException("User not found"));
        userRepository.delete(user);
        jedis.del(String.valueOf(user.getId()));
        System.out.println("delete in cache");
        System.out.println("Delete complete!");
    }
}