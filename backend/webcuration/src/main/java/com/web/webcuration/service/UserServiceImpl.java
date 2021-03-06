package com.web.webcuration.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import com.web.webcuration.Entity.Provide;
import com.web.webcuration.Entity.User;
import com.web.webcuration.dto.SearchUserInfo;
import com.web.webcuration.dto.request.NickNameRequest;
import com.web.webcuration.dto.request.ProfileRequest;
import com.web.webcuration.dto.request.UserRequest;
import com.web.webcuration.dto.response.BaseResponse;
import com.web.webcuration.dto.response.UserRelationListResponse;
import com.web.webcuration.repository.UserQueryRepository;
import com.web.webcuration.repository.UserRepository;
import com.web.webcuration.textMatcher.KoreanTextMatch;
import com.web.webcuration.textMatcher.KoreanTextMatcher;
import com.web.webcuration.utils.FileUtils;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserQueryRepository userQueryRepository;

    @Override
    public User getUserInfo(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            return user.get();
        } else {
            return null;
        }
    }

    @Override
    public User getUserInfo(Long userid) {
        Optional<User> user = userRepository.findById(userid);
        if (user.isPresent()) {
            return user.get();
        } else {
            return null;
        }
    }

    @Override
    public BaseResponse deleteUser(Long userid) {
        userRepository.deleteById(userid);
        return BaseResponse.builder().status("200").status("success").build();
    }

    @Override
    @Transactional
    public BaseResponse updateUser(ProfileRequest profileRequest) throws IllegalStateException, IOException {
        if (userQueryRepository.DuplicateCheckName(profileRequest.getId(), profileRequest.getNickname())) {
            Optional<User> user = userRepository.findById(profileRequest.getId());
            if (user.isPresent()) {
                // ???????????? ?????? ?????? ??????
                User changeUser = user.get();
                changeUser.setNickname(profileRequest.getNickname());
                changeUser.setIntroduce(profileRequest.getIntroduce());

                if (profileRequest.isFileChanged()) {
                    // ????????? ?????? ?????????
                    if (!changeUser.getImage().equals("/profileImg/user_image.png")) {
                        // ?????? ???????????? ?????????
                        if (changeUser.getProvide().equals(Provide.LOCAL)) {
                            // ????????????
                            FileUtils.deleteProfile(changeUser.getImage());
                        } else {
                            // SNS???
                            changeUser.setProvide(Provide.LOCAL);
                        }
                    }
                    if (profileRequest.getImage() == null) {
                        // ?????? ????????? ????????? ?????????
                        changeUser.setImage("/profileImg/user_image.png");
                    } else {
                        // ?????? ???????????? ???????????????
                        changeUser.setImage(FileUtils.uploadProfile(profileRequest.getImage()));
                    }
                }
                return BaseResponse.builder().status("200").status("success").data(userRepository.save(changeUser))
                        .build();
            }
            throw new RuntimeException("??????????????? ????????? ????????????.");
        } else {
            return BaseResponse.builder().status("500").msg("????????? ??????").build();
        }
    }

    @Override
    @Transactional
    public BaseResponse updatePasswordUser(UserRequest changeUser) {
        Optional<User> user = userRepository.findByEmail(changeUser.getEmail());
        if (user.isPresent()) {
            user.get().setPassword(passwordEncoder.encode(changeUser.getPassword()));

            return BaseResponse.builder().status("200").status("success").data(userRepository.save(user.get())).build();
        } else {
            throw new RuntimeException("???????????? ?????? ??????");
        }
    }

    @Override
    public BaseResponse setNickname(NickNameRequest nicknameRequest) {
        if (userQueryRepository.DuplicateCheckName(nicknameRequest.getId(), nicknameRequest.getNickname())) {
            Optional<User> user = userRepository.findById(nicknameRequest.getId());
            user.get().setNickname(nicknameRequest.getNickname());
            return BaseResponse.builder().status("200").msg("success").data(userRepository.save(user.get())).build();
        } else {
            return BaseResponse.builder().status("500").msg("????????? ??????").build();
        }
    }

    @Override
    public User createUser(User newUser) {
        return userRepository.save(newUser);
    }

    // ?????? ??????
    @Override
    public List<SearchUserInfo> getSearchNickname(List<Long> block, String text) {
        List<User> UserOrderBy = userQueryRepository.getUserOrderBy(block);
        List<SearchUserInfo> searchUser = new ArrayList<>();
        KoreanTextMatcher matcher = new KoreanTextMatcher(text);
        for (User user : UserOrderBy) {
            KoreanTextMatch match = matcher.match(user.getNickname());
            if (match.success()) {
                searchUser.add(SearchUserInfo.builder().id(user.getId()).image(user.getImage()).name(user.getNickname())
                        .build());
                if (searchUser.size() == 5) {
                    break;
                }
            }
        }
        return searchUser;
    }

    @Override
    public void changeUserFollowing(Long userid, Long number) {
        Optional<User> previousUser = userRepository.findById(userid);
        if (previousUser.isPresent()) {
            User user = previousUser.get();
            Long changeFollow = user.getFollowing() + number;
            user.setFollowing(changeFollow);
            userRepository.save(user);
        } else {
            throw new RuntimeException("?????? ????????? ????????????.");
        }
    }

    @Override
    public void changeUserFollower(Long userid, Long number) {
        Optional<User> previousUser = userRepository.findById(userid);
        if (previousUser.isPresent()) {
            User user = previousUser.get();
            Long changeFollower = user.getFollower() + number;
            user.setFollower(changeFollower);
            userRepository.save(user);
        } else {
            throw new RuntimeException("?????? ????????? ????????????.");
        }
    }

    // ?????? ?????? ????????????
    @Override
    public List<User> getRankUsers(List<Long> block) {
        return userQueryRepository.getRankUsers(block);
    }

    // ?????????, ????????? ?????? ?????? ??????
    @Override
    public BaseResponse getRelationToUser(HashMap<Long, String> states) {
        if (states.size() == 0) {
            List<User> data = new ArrayList<>();
            return BaseResponse.builder().status("200").msg("success").data(data).build();
        } else {
            List<User> relationUser = userQueryRepository.getListToUser(states);
            List<UserRelationListResponse> userRelationListResponses = new ArrayList<>();
            for (User user : relationUser) {
                userRelationListResponses
                        .add(UserRelationListResponse.builder().user(user).state(states.get(user.getId())).build());
            }
            return BaseResponse.builder().status("200").msg("success").data(userRelationListResponses).build();
        }
    }

    @Override
    public List<UserRelationListResponse> getRandomUserInfo(List<Long> block, Long userid) {
        List<User> otherUsers = userQueryRepository.getOtherUser(block, userid);
        int otherUserSize = otherUsers.size();
        Collections.shuffle(otherUsers);
        List<UserRelationListResponse> randomOtherUsers = new ArrayList<>();
        for (int i = 0; i < (otherUserSize <= 10 ? otherUserSize : 10); i++) {
            randomOtherUsers.add(UserRelationListResponse.builder().user(otherUsers.get(i)).state("no").build());
        }
        return randomOtherUsers;
    }

    @Override
    public boolean CheckNickname(String nickname) {
        return userQueryRepository.DuplicateCheckName(nickname);
    }
}
