package com.example;

public class UserConverter {

    public static UserEntity convertUserRequestDtoToEntity(UserRequestDto userRequestDto){
        return UserEntity.builder().userName(userRequestDto.getUserName()).name(userRequestDto.getName())
                .email(userRequestDto.getEmail()).mobileNo(userRequestDto.getMobileNo())
                .build();
    }

    public static UserRequestDto convertEntityToRequestDto(UserEntity user){
        return UserRequestDto.builder().userName(user.getUserName()).name(user.getName())
                .email(user.getEmail()).mobileNo(user.getMobileNo())
                .build();
    }
}
