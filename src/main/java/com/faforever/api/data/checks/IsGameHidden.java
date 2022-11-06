package com.faforever.api.data.checks;

import com.faforever.api.data.domain.Game;
import com.faforever.api.data.domain.OwnableEntity;
import com.faforever.api.security.ElideUser;
import com.faforever.api.security.FafUserDetails;
import com.yahoo.elide.annotation.SecurityCheck;
import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.filter.Operator;
import com.yahoo.elide.core.filter.expression.FilterExpression;
import com.yahoo.elide.core.filter.predicates.FilterPredicate;
import com.yahoo.elide.core.security.ChangeSpec;
import com.yahoo.elide.core.security.RequestScope;
import com.yahoo.elide.core.security.checks.FilterExpressionCheck;
import com.yahoo.elide.core.security.checks.OperationCheck;
import com.yahoo.elide.core.type.Type;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

public class IsGameHidden {

  public static final String EXPRESSION = "isGameHidden";

  @SecurityCheck(EXPRESSION)
  public static class Inline extends FilterExpressionCheck<Game> {

    @Override
    public FilterExpression getFilterExpression(Type<?> type, RequestScope requestScope) {
      Path.PathElement entityHiddenPath = new Path.PathElement(Game.class, Boolean.class, "replayHidden");
      return new FilterPredicate(entityHiddenPath, Operator.IN, Collections.singletonList(true));
    }
  }
}
