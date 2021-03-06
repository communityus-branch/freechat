package vn.team.freechat.common.service.impl;

import java.util.List;

import com.tvd12.ezyfox.bean.annotation.EzyAutoBind;
import com.tvd12.ezyfox.bean.annotation.EzySingleton;
import com.tvd12.ezyfox.function.EzyApply;

import lombok.Setter;
import vn.team.freechat.common.constant.ChatEntities;
import vn.team.freechat.common.constant.ChatLockKeys;
import vn.team.freechat.common.data.ChatNewUser;
import vn.team.freechat.common.data.ChatUser;
import vn.team.freechat.common.repo.ChatUserRepo;
import vn.team.freechat.common.service.ChatUserService;
import vn.team.freechat.common.service.HazelcastMapHasMaxIdService;

@Setter
@EzySingleton("userService")
public class ChatUserServiceImpl 
		extends HazelcastMapHasMaxIdService<String, ChatUser> 
		implements ChatUserService {

	@EzyAutoBind
	private ChatUserRepo userRepo;
	
	@Override
	public void saveUser(ChatUser user) {
		set(user.getUsername(), user);
		
	}
	
	@Override
	public ChatUser getUser(String username) {
		return get(username);
	}
	
	@Override
	public ChatNewUser createUser(String username, EzyApply<ChatUser> applier) {
		ChatUser user = getUser(username);
		if(user != null) 
			return new ChatNewUser(user, false);
		return lockUpdateAndGet(getNewLockKey(username), () -> {
			ChatUser cuser = getUser(username);
			if(cuser != null) 
				return new ChatNewUser(user, false);
			ChatUser nuser = newUser(username);
			applier.apply(nuser);
			map.set(username, nuser);
			return new ChatNewUser(nuser, true);
		});
	}
	
	@Override
	public List<ChatUser> getSuggestionUsers(String owner, int skip, int limit) {
		return userRepo.findSuggestionUsers(owner, skip, limit);
	}
	
	private ChatUser newUser(String username) {
		ChatUser user = new ChatUser();
		user.setId(newId(ChatEntities.CHAT_USER));
		user.setUsername(username);
		return user;
	}
	
	@Override
	protected String getMapName() {
		return ChatEntities.CHAT_USER;
	}
	
	private String getNewLockKey(String username) {
		String lockKey = username + ChatLockKeys.NEW_USER_SUFFIX;
		return lockKey;
	}
}
