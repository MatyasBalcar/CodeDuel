package dev.codeduel.problems;

import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/problems")
public class ProblemController {
  private final ProblemService problems;

  public ProblemController(ProblemService problems) {
    this.problems = problems;
  }

  @GetMapping
  List<ProblemService.Summary> list() {
    return problems.catalog();
  }

  @GetMapping("/{slug}")
  ProblemService.Detail detail(@PathVariable String slug) {
    return problems.published(slug);
  }
}
