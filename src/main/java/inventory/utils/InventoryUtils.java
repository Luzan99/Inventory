package inventory.utils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class InventoryUtils {

    private InventoryUtils(){

    }

    public static ResponseEntity<String> getResponseEntity(String responseMessage, HttpStatus httpStatus){
        return new ResponseEntity<String>("{\"message\":\""+responseMessage+"\"}", httpStatus);
    }
}
