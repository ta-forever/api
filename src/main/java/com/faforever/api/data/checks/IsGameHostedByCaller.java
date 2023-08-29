package com.faforever.api.data.checks;

import com.faforever.api.data.domain.Game;
import com.faforever.api.data.domain.Login;
import com.faforever.api.data.domain.OwnableEntity;
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

public class IsGameHostedByCaller {

  public static final String EXPRESSION = "isGameHostedByCaller";

  @SecurityCheck(EXPRESSION)
  public static class Inline extends FilterExpressionCheck<Game> {

    @Override
    public FilterExpression getFilterExpression(Type<?> type, RequestScope requestScope) {
      final ElideUser caller = (ElideUser) requestScope.getUser();
      final Integer callerId = caller.getFafUserDetails().map(FafUserDetails::getId).orElse(0);

      final Path.PathElement hostPath = new Path.PathElement(Game.class, Player.class, "host");
      final Path.PathElement loginIdPath = new Path.PathElement(Player.class, Integer.class, "id");
      final List<Path.PathElement> pathList = Arrays.asList(hostPath, loginIdPath);
      final Path path = new Path(pathList);
      return new FilterPredicate(path, Operator.IN, Collections.singletonList(callerId));
    }
  }
}
