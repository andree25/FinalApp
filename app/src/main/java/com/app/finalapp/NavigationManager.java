package com.app.finalapp;

import java.util.Stack;

public class NavigationManager {
    private static NavigationManager instance;
    private Stack<Integer> navigationStack = new Stack<>();

    private NavigationManager() {
        // Private constructor to prevent instantiation
    }

    public static synchronized NavigationManager getInstance() {
        if (instance == null) {
            instance = new NavigationManager();
        }
        return instance;
    }

    public void pushFragmentId(int fragmentId) {
        if (!navigationStack.isEmpty() && navigationStack.peek() != fragmentId) {
            navigationStack.push(fragmentId);
        }
    }

    public Integer popFragmentId() {
        return !navigationStack.isEmpty() ? navigationStack.pop() : null;
    }

    public Integer getCurrentFragmentId() {
        return !navigationStack.isEmpty() ? navigationStack.peek() : null;
    }
}
