package tw.edu.ntub.imd.camping.databaseconfig.entity.listener;

import javax.persistence.PrePersist;
import java.time.LocalDateTime;

public class UserCommentListener {

    @PrePersist
    private void preSave(UserComment userComment) {
        if (userComment.getCommentDate() == null) {
            userComment.setCommentDate(LocalDateTime.now());
        }
    }
}
