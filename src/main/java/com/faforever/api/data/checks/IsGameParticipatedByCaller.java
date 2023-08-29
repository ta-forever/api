package com.faforever.api.data.checks;

import com.faforever.api.data.domain.Game;
import com.faforever.api.data.domain.GamePlayerStats;
import com.faforever.api.data.domain.Player;
import com.faforever.api.security.ElideUser;
import com.faforever.api.security.FafUserDetails;
import com.yahoo.elide.annotation.SecurityCheck;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.security.checks.FilterExpressionCheck;
import com.yahoo.elide.core.type.Type;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class IsGameParticipatedByCaller {

  public static final String EXPRESSION = "isGameParticipatedByCaller";

  @SecurityCheck(EXPRESSION)
  public static class Inline extends FilterExpressionCheck<Game> {

    @Override
    public FilterExpression getFilterExpression(Type<?> type, RequestScope requestScope) {
      final ElideUser caller = (ElideUser) requestScope.getUser();
      final Integer callerId = caller.getFafUserDetails().map(FafUserDetails::getId).orElse(0);
      final Path.PathElement playerStatsPath = new Path.PathElement(Game.class, GamePlayerStats.class, "playerStats");
      final Path.PathElement playerPath = new Path.PathElement(GamePlayerStats.class, Player.class, "player");
      final Path.PathElement idPath = new Path.PathElement(Player.class, Integer.class, "id");
      final List<Path.PathElement> pathList = Arrays.asList(playerStatsPath, playerPath, idPath);
      final Path path = new Path(pathList);
      return new FilterPredicate(path, Operator.HASMEMBER, Collections.singletonList(callerId));
    }
  }
}
