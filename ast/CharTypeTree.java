/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ast;
import visitor.*;

/**
 *
 * @author Auzz
 */

public class CharTypeTree extends AST {
    public CharTypeTree() {
    }

    public Object accept(ASTVisitor v) {
        return v.visitCharTypeTree(this);
    }
}
