package tw.edu.ntub.imd.camping.databaseconfig.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import tw.edu.ntub.imd.camping.databaseconfig.Config;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 使用者評價
 *
 * @since 1.0.0
 */
@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "user_comment", schema = Config.DATABASE_NAME)
public class UserComment {
    /**
     * 流水編號
     *
     * @since 1.0.0
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, columnDefinition = "UNSIGNED")
    private Integer id;

    /**
     * 評價的租借紀錄編號
     *
     * @since 1.7.0
     */
    @Column(name = "rental_record_id", nullable = false)
    private Integer rentalRecordId;

    /**
     * 被評價的使用者帳號
     *
     * @since 1.0.0
     */
    @Column(name = "user_account", length = 100, nullable = false)
    private String userAccount;

    /**
     * 評價(0 ~ 5)
     *
     * @since 1.0.0
     */
    @Column(name = "comment", nullable = false, columnDefinition = "UNSIGNED")
    private Byte comment;

    /**
     * 評價者帳號
     *
     * @since 1.0.0
     */
    @CreatedBy
    @Column(name = "comment_account", length = 100, nullable = false)
    private String commentAccount;

    /**
     * 評價時間
     *
     * @since 1.0.0
     */
    @CreatedDate
    @Column(name = "comment_date", nullable = false)
    private LocalDateTime commentDate;

    /**
     * 評價的租借紀錄
     *
     * @see RentalRecord
     * @since 1.7.0
     */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_record_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false)
    private RentalRecord rentalRecord;

    /**
     * 被評價的使用者
     *
     * @see User
     * @since 1.0.0
     */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account", referencedColumnName = "account", nullable = false, insertable = false, updatable = false)
    private User userByUserAccount;

    /**
     * 評價者
     *
     * @since 1.0.0
     */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_account", referencedColumnName = "account", nullable = false, insertable = false, updatable = false)
    private User userByCommentAccount;
}
