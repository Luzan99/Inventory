package inventory.dao;

import inventory.POJO.User;
import inventory.wrapper.UserWrapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface UserDao extends JpaRepository<User, Integer> {

    @Query(value = "select u from User u where u.email=:email")
    User findByEmailId(@Param("email") String email);

    @Query(value = "select new inventory.wrapper.UserWrapper(u.id,u.name,u.email,u.contactNumber,u.status) from User u where u.role='user'")
    List<UserWrapper> getAllUser();

    @Query(value = "select  u.email from User u where u.role=:role")
    List<String> findUsersByRole(String role);

    @Transactional
    @Modifying
    @Query(value = "update User u set u.status=:status where u.id=:id")
    Integer updateStatus(@Param("status") String status, @Param("id") Integer id);
}
