package hu.rxd.checkstyle;

import com.google.common.collect.Sets;
import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.utils.CheckUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class FunctionArgsAlignmentRule extends AbstractCheck
{
  public static final String ARGS_MISALIGNED = "arguments.misaligned";
  public static final String ARGS_MIXED_LINES = "arguments.mixed.lines";
  private Set<String> include = Collections.emptySet();
  private Set<String> exclude = Collections.emptySet();
  private boolean columnAlignment = true;
  private boolean columnAlignmentFirst = false;

  /**
   * Excluded function call names - separated with ','.
   *
   * Non-qualified method names; no package/class names!
   *
   * @param names
   */
  public void setExclude(String names)
  {
    this.exclude = Sets.newHashSet(names.split(","));
  }

  /**
   * Excluded function call names - separated with ','.
   *
   * Non-qualified method names; no package/class names!
   *
   * @param names
   */
  public void setInclude(String names)
  {
    this.include = Sets.newHashSet(names.split(","));
  }

  /**
   * Enables to check column alignment of arguments on separate lines.
   */
  public void setColumnAlignment(boolean enable)
  {
    columnAlignment = enable;
  }

  /**
   * This may allow the first argument to remain next to the function call.
   */
  public void setColumnAlignmentFirst(boolean enable)
  {
    columnAlignmentFirst = enable;
  }

  @Override
  public int[] getRequiredTokens()
  {
    return new int[] {TokenTypes.METHOD_CALL};
  }

  @Override
  public int[] getDefaultTokens()
  {
    return getRequiredTokens();
  }

  @Override
  public int[] getAcceptableTokens()
  {
    return getRequiredTokens();
  }

  @Override
  public void visitToken(DetailAST astCall)
  {
      String name = collectMethodNames(astCall.getFirstChild());
      // DetailAST method = astCall.getFirstChild();
      // method.get
      // throw new RuntimeException(name);
      if(!include.isEmpty() && !include.contains(name)) {
        return;
      }
      if (exclude.contains(name)) {
//        throw new RuntimeException(name);
        return;
      }
    DetailAST args = astCall.findFirstToken(TokenTypes.ELIST);
    if (args.getChildCount() == 0) {
      return;
    }
    int childCount = 0;
    Set<Integer> childLineNos = new HashSet<>();
    Set<Integer> childColNos = new HashSet<>();

    for (DetailAST ast = args.getFirstChild(); ast != null; ast = ast.getNextSibling()) {
      if (ast.getType() != TokenTypes.EXPR) {
        continue;
      }
      DetailAST firstNode = CheckUtil.getFirstNode(ast);
      childCount++;
      childLineNos.add(firstNode.getLineNo());
      if (columnAlignmentFirst || childCount > 1) {
        childColNos.add(firstNode.getColumnNo());
      }
    }

    if (childLineNos.size() == 1) {
      // ok; all args on same line
      return;
    }

    if (childCount != childLineNos.size()) {
      log(astCall, ARGS_MIXED_LINES, name);
      return;
    }

    if (columnAlignment && childColNos.size() > 1) {
      log(astCall, ARGS_MISALIGNED, name);
    }
  }

  /**
   * Returns the first column which is occupied by this ast node in its line.
   *
   * @param ast
   * @return
   */
  private int getFirstColumn(DetailAST ast)
  {
    int ret = ast.getColumnNo();
    for (DetailAST child = ast.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child.getLineNo() != ast.getLineNo()) {
        continue;
      }
      ret = Math.min(ret, getFirstColumn(child));
    }
    return ret;
  }

  private String collectMethodNames(DetailAST ast)
  {
    switch (ast.getType())
    {
    case TokenTypes.DOT:
      return collectMethodNames(ast.getLastChild());
    case TokenTypes.IDENT:
      return ast.getText();
    default:
      throw new RuntimeException("unexpected type");
    }

  }
}
