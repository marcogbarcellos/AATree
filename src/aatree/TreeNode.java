/**
 * @license
 * Copyright 2017 The FOAM Authors. All Rights Reserved.
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package aatree;

import java.util.Arrays;

public class TreeNode {
  protected Object key;
  protected Object value;
  protected long size;                          
  protected long level;                 
  protected TreeNode left;
  protected TreeNode right;
  
  protected static TreeNode nullNode;
  
  public TreeNode(Object key, Object value) {
    this.key = key;
    this.value = value;
  }
  
  public TreeNode(Object key, Object value, long size, long level,
                  TreeNode left, TreeNode right) {
    this.key = key;
    this.value = value;
    this.size = size;
    this.level = level;
    this.left = left;
    this.right = right;
  }
  public TreeNode cloneNode() {
    return new TreeNode(this.key, this.value, this.size, 
                               this.level, this.left, this.right);
    
  } 
  private TreeNode maybeClone(TreeNode s) {
    if ( s != null ) {
      return s.cloneNode();
    }
    return s;
  }
  public static TreeNode getNullNode() {
    if ( nullNode != null ) {
      return nullNode;
    }
    nullNode = new TreeNode(null, null);
    nullNode.level = nullNode.size = 0;
    nullNode.left = nullNode.right = null;
    return nullNode;
  }
  
  public Object bulkLoad(TreeNode tree, Object... a) {
    // Only safe if children aren't themselves trees
    //if ( this.tail === ValueIndex ) {
    if ( true ) {
      Arrays.sort(a);
      return this.bulkLoad_(tree, 0, a.length-1, a);
    }
      
    for ( int i = 0; i < a.length; i++ ) {
        tree = putKeyValue(tree, a[i], a[i]);
    }
    return tree;
  }
  
  public Object bulkLoad_(TreeNode t, int start, int end, Object... a) {
    if( end < start ) {
      return null;
    }
    int m = start + (int)Math.floor((end-start+1)/2);
    
    TreeNode tree = putKeyValue(t, a[m], a[m]);
    tree.left = (TreeNode)bulkLoad_(tree.left, start, m-1, a);
    tree.right = (TreeNode)bulkLoad_(tree.right, m+1, end, a);
    tree.size += size(tree.left) + size(tree.right);
    return tree;
  }
  
  public TreeNode putKeyValue(TreeNode state, Object key,
    Object value) {
    if ( state == null || state.equals(TreeNode.getNullNode()) ) {
      return new TreeNode(key, value, 1, 1, null, null);
    }
    state = maybeClone(state);  
    
    int r = compare(state.key, key);
    
    if ( r != 0 ) {
      if ( r > 0 ) {
        if ( state.left != null ) {
            state.size -= state.left.size;
        }
        state.left = putKeyValue(state.left, key, value);
        state.size += state.left.size;
        
      } else {
        if ( state.right != null ) {
            state.size -= state.right.size;
        }
        state.right = putKeyValue(state.right, key, value);
        state.size += state.right.size;
      }
    }
    return split(skew(state));
  }
  
  public TreeNode skew(TreeNode node) {
    if ( node != null && node.left != null && node.left.level == node.level ) {
      // Swap the pointers of horizontal left links.
      TreeNode l = maybeClone(node.left);
      
      node.left = l.right;
      l.right = node;
      
      updateSize(node);
      updateSize(l);
      return l;
    }
    return node;
  }
  
  public TreeNode split(TreeNode node) {
    if ( node != null && node.right != null && node.right.right != null &&
        node.level == node.right.right.level ) {
      // Swap the pointers of horizontal left links.
      TreeNode r = maybeClone(node.right);
      
      node.right = r.left;
      r.left = node;
      r.level++;
      
      updateSize(node);
      updateSize(r);
      return r;
    }
    return node;
  }
  
  public TreeNode removeKeyValue(TreeNode state, Object key,
    Object value) {
    if ( state == null ) {
      return state;
    }
    
    state = maybeClone(state);
    long compareValue = compare(state.key, key);
    
    if ( compareValue == 0 ) {
      state.size--;
      if ( state.left == null && state.right == null ) {
        state = null;
        return state;
      }
      boolean isLeft = ( state.left != null );
      TreeNode subs = isLeft ? predecessor(state) : successor(state);
      state.key = subs.key;
      state.value = subs.value;
      if( isLeft ) {
        state.left = removeNode(state.left, subs.key);
      } else {
        state.right = removeNode(state.right, subs.key);
      }
    } else {
      if ( compareValue > 0 ) {
        state.size -= size(state.left);
        state.left = removeKeyValue(state.left, key, value);
        state.size += size(state.left);  
      } else {
        state.size -= size(state.right);
        state.right = removeKeyValue(state.right, key, value);
        state.size += size(state.right);    
      }
    }
    // Rebalance the tree. Decrease the level of all nodes in this level if
    // necessary, and then skew and split all nodes in the new level.
    state = skew(decreaseLevel(state));
    if ( state.right != null ) {
      state.right = skew(maybeClone(state.right));
      if ( state.right.right != null ) {
        state.right.right = skew(maybeClone(state.right.right));
      }
    }
    state = split(state);
    state.right = split(maybeClone(state.right));
    
    return state;
  }
  
  private TreeNode removeNode(TreeNode state, Object key) {
    if ( state == null ) {
      return state;
    }
    state  = maybeClone(state);
    long compareValue = compare(state.key, key);
    
    if ( compareValue == 0 ) {
      return state.left != null ? state.left : state.right;
    }
    if ( compareValue > 0 ) {
      removeSideNode(state, state.left, key);
    } else {
      removeSideNode(state, state.right, key);
    }
    return state;
  }
  
  private void removeSideNode(TreeNode parent, TreeNode side,
    Object key) {
    parent.size -= size(side);
    side = removeNode(side, key);
    parent.size += size(side);
  }
  
  private TreeNode predecessor(TreeNode node) {
    if ( node.left == null ) {
      return node;
    }
    node = node.left;
    while ( node.right != null ) {
      node = node.right;
    }
    return node;
  }
  
  private TreeNode successor(TreeNode node) {
    if ( node.right == null ) {
      return node;
    }
    node = node.right;
    while ( node.left != null ) {
      node = node.left;
    }
    return node;
  }
  
  private TreeNode decreaseLevel(TreeNode node) {
    long expectedLevel = 1 + Math.min(
      node.left != null ? node.left.level : 0 ,
      node.right != null ? node.right.level : 0);
    
    if ( expectedLevel < node.level ) {
      node.level = expectedLevel;
      if ( node.right != null && expectedLevel < node.right.level ) {
        node.right = maybeClone(node.right);
        node.right.level = expectedLevel;
      }
    }
    return node;
  }
  
  private void updateSize(TreeNode node) {
    node.size = size(node.left) + size(node.right) + 1;
  }
  
  private long size (TreeNode node) {
    if ( node != null ) {
      return node.size;
    }
    return 0;
  }
  
  public Object get(TreeNode s, Object key) {
    if ( s == null ) {
      return s;
    }
    int r = compare(s.key, key);
    if ( r == 0 ) {
      return s.value;
    } else if ( r > 0 ) {
      return get(s.left, key);
    } else {
      return get(s.right, key);
    }
  }
  public TreeNode gt(TreeNode s, Object key) {
    if ( s == null ) {
      return s;
    }
    int r = compare(s.key, key);
    if ( r < 0 ) {
      TreeNode l = gt(s.left, key);
      long newSize = size(s) - size(s.left) + size(l);
      return new TreeNode(s.key, s.value, newSize, 
        s.level, l, s.right);
    } 
    if ( r > 0 ) {
      return gt(s.right, key);
    }
    
    return s.right;
  }
  
  public TreeNode gte(TreeNode s, Object key) {
    if ( s == null ) {
      return s;
    }
    int r = compare(s.key, key);
    if ( r < 0 ) {
      TreeNode l = gte(s.left, key);
      long newSize = size(s) - size(s.left) + size(l);
      return new TreeNode(s.key, s.value, newSize, 
        s.level, l, s.right);
    } 
    if ( r > 0 ) {
      return gte(s.right, key);
    }
    
    return new TreeNode(s.key, s.value, size(s) - size(s.left), 
      s.level, null, s.right);
  }
  public TreeNode lt(TreeNode s, Object key) {
    if ( s == null ) {
      return s;
    }
    int r = compare(s.key, key);
    if ( r > 0 ) {
      TreeNode right = lt(s.right, key);
      long newSize = size(s) - size(s.right) + size(right);
      return new TreeNode(s.key, s.value, newSize, 
        s.level, s.left, right);
    } 
    if ( r < 0 ) {
      return lt(s.left, key);
    }
    
    return  s.left;
  }
  public TreeNode lte(TreeNode s, Object key) {
    if ( s == null ) {
      return s;
    }
    int r = compare(s.key, key);
    if ( r > 0 ) {
      TreeNode right = lte(s.right, key);
      long newSize = size(s) - size(s.right) + size(right);
      return new TreeNode(s.key, s.value, newSize, 
        s.level, s.left, right);
    } 
    if ( r < 0 ) {
      return lte(s.left, key);
    }
    
    return new TreeNode( s.key, s.value, size(s) - size(s.right), 
      s.level, s.left, null);
  }
  //if integer type, compare direct, otherwise compares through hashcode.
  public int compare(Object o1, Object o2 ) {
    if(o1 instanceof Integer && o2 instanceof Integer) {
      if ( (Integer)o1 == (Integer)o2) {
          return 0;
      } else if ( (Integer)o1 > (Integer)o2 ) {
        return 1;
      }
      return -1;
    } else {
      if( o1.equals(o2) ) {
        return 0;
      } else if ( o1.hashCode() > o2.hashCode() ) {
        return 1;
      }
      return -1;   
    }
  }
  
  public static void main(String[] args) {
      //String[] v = {"a","b","c","d","e","f","g","h","i"};
      Integer[] v = {99,88,77,165,33,89,24,49,245};
      
      TreeNode tree = TreeNode.getNullNode();
      tree = (TreeNode)tree.bulkLoad(tree, v);
  
      tree = (TreeNode)tree.removeKeyValue(tree, 33, 33);
      System.out.println("and now?");
  }
}
