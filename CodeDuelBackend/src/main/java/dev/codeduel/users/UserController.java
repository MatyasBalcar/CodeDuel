package dev.codeduel.users;

import dev.codeduel.auth.AccountPrincipal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class UserController {
  private final UserService users;

  public UserController(UserService users) {
    this.users = users;
  }

  @GetMapping("/leaderboard")
  List<UserService.Profile> leaderboard() {
    return users.leaderboard();
  }

  @GetMapping("/users/{username}")
  UserService.Profile profile(@PathVariable String username) {
    return users.profile(username);
  }

  @GetMapping("/users/{username}/rating-history")
  List<UserService.RatingPoint> ratings(@PathVariable String username) {
    return users.ratingHistory(username);
  }

  @GetMapping("/users/me/matches")
  List<UserService.MatchHistory> history(@AuthenticationPrincipal AccountPrincipal user) {
    return users.history(user.id());
  }
}
