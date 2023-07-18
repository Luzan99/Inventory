package inventory.POJO;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = "user")
public class User implements Serializable {
    private static final long serialVersionUID = 1l;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    // User name
    @Column(name = "name")
    private String name;

    //Number
    @Column(name = "contactNumber")
    private String contactNumber;

    //Email Address
    @Column(name = "email")
    private String email;

    //Password
    @Column(name = "password")
    private String password;

    //Status
    @Column(name = "status")
    private String status;

    //Roles
    @Column(name = "role")
    private String role;
}
