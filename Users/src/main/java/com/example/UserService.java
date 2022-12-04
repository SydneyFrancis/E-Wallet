package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
public class UserService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    KafkaTemplate<String,String> kafkaTemplate;

    public final String REDIS_PREFIX_KEY = "user::";

    public final String CREATE_WALLET_TOPIC = "create_wallet";
    public void createUser(UserRequestDto userRequestDto){
        UserEntity user = UserConverter.convertUserRequestDtoToEntity(userRequestDto);


        userRepository.save(user);

        saveInCache(user);

        JSONObject walletRequest = new JSONObject();

        walletRequest.put("username",user.getUserName());

        walletRequest.put("name",user.getName());

        String message = walletRequest.toString();
        kafkaTemplate.send(CREATE_WALLET_TOPIC,message);
    }

    public String getEmail(String userName){
        UserEntity user = userRepository.findByUserName(userName);
        return user.getEmail();
    }

    private void saveInCache(UserEntity user){

        Map map = objectMapper.convertValue(user,Map.class);

        redisTemplate.opsForHash().putAll(REDIS_PREFIX_KEY+user.getUserName(),map);

        redisTemplate.expire(REDIS_PREFIX_KEY+user.getUserName(), Duration.ofHours(12));
    }

    public UserEntity getUser(String userName) throws UserNotFoundException {

        Map map = redisTemplate.opsForHash().entries(REDIS_PREFIX_KEY+userName);
        if(map.size() != 0){
            UserEntity object = objectMapper.convertValue(map,UserEntity.class);
            return object;
        }
        else {

            try {
                UserEntity user = userRepository.findByUserName(userName);
                if (user == null) {
                    throw new UserNotFoundException();
                }
                saveInCache(user);
                return user;
            } catch (Exception e) {
                throw new UserNotFoundException();
            }
        }
    }
}
