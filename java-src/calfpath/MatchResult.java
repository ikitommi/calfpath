//   Copyright (c) Shantanu Kumar. All rights reserved.
//   The use and distribution terms for this software are covered by the
//   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
//   which can be found in the file LICENSE at the root of this distribution.
//   By using this software in any fashion, you are agreeing to be bound by
//   the terms of this license.
//   You must not remove this notice, or any other, from this software.


package calfpath;

import java.util.AbstractList;
import java.util.Collections;
import java.util.Map;
import java.util.RandomAccess;

public class MatchResult extends AbstractList<Object> implements RandomAccess {

    public static final MatchResult NO_MATCH = null;

    @SuppressWarnings("unchecked")
    public static final Map<?, String> NO_PARAMS = Collections.EMPTY_MAP;

    public static final int FULL_MATCH_INDEX = -1;

    public static final MatchResult FULL_MATCH_NO_PARAMS = new MatchResult(NO_PARAMS, FULL_MATCH_INDEX);

    private final Map<?, String> params;

    /** End index in the URI when match stopped. -1 implies match fully ended. */
    private final int endIndex; // excluding

    protected MatchResult(Map<?, String> params, int endIndex) {
        this.params = params;
        this.endIndex = endIndex;
    }

    // ----- factory methods -----

    public static MatchResult partialMatch(Map<?, String> params, int endIndex) {
        return new MatchResult(params, endIndex);
    }

    public static MatchResult partialMatch(int endIndex) {
        return new MatchResult(NO_PARAMS, endIndex);
    }

    public static MatchResult fullMatch(Map<?, String> params) {
        return new MatchResult(params, FULL_MATCH_INDEX);
    }

    // ----- utility methods -----

    public Map<?, String> getParams() {
        return params;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public boolean isFullMatch() {
        return endIndex == FULL_MATCH_INDEX;
    }

    // ----- java.util.List methods -----

    @Override
    public Object get(int index) {
        switch (index) {
        case 0: return params;
        case 1: return endIndex;
        default: throw new IndexOutOfBoundsException("Expected index 0 or 1, but found " + index);
        }
    }

    @Override
    public boolean isEmpty() { return false; }

    @Override
    public int size() { return 2; }

    // ----- overridden methods -----

    @Override
    public String toString() {
        return String.format("params: %s, endIndex: %d", params, endIndex);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MatchResult) {
            MatchResult other = (MatchResult) obj;
            return other.params.equals(params) && other.endIndex == endIndex;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

}
