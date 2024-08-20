package gov.nasa.podaac.swodlr.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import gov.nasa.podaac.swodlr.exception.SwodlrException;

@Controller
public class UserController {
  @Autowired
  UserRepository userRepository;

  @QueryMapping
  public User currentUser(@ContextValue UserReference userRef) {
    return userRef.fetch();
  }

  @PreAuthorize("hasRole(\"ROLE_Administrator\")")
  @QueryMapping
  public User user(@Argument String username) {
    var result = userRepository.findByUsername(username);
    if (result.isEmpty()) {
      throw new SwodlrException("User not found");
    }

    User user = result.get();
    return user;
  }
}
