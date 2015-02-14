/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.addon.as.jboss.common.util;

import java.io.File;
import java.util.ResourceBundle;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class Messages {
    public static final Messages INSTANCE = new Messages();

    private static final int INITIAL = 0;
    private static final int GOT_DOLLAR = 1;
    private static final int GOT_OPEN_BRACE = 2;
    private static final int RESOLVED = 3;
    private static final int DEFAULT = 4;

    private final ResourceBundle bundle;

    private Messages() {
        bundle = ResourceBundle.getBundle(Messages.class.getName());
    }

    public String getMessage(final String key) {
        return resolveExpression(bundle.getString(key));
    }

    public String getMessage(final String key, final Object... args) {
        return resolveExpression(String.format(bundle.getString(key), args));
    }

    private String resolveExpression(final String expression) {
        if (expression == null) return null;
        final StringBuilder builder = new StringBuilder();
        final char[] chars = expression.toCharArray();
        final int len = chars.length;
        int state = 0;
        int start = -1;
        int nameStart = -1;
        for (int i = 0; i < len; i++) {
            char ch = chars[i];
            switch (state) {
                case INITIAL: {
                    switch (ch) {
                        case '$': {
                            state = GOT_DOLLAR;
                            continue;
                        }
                        default: {
                            builder.append(ch);
                            continue;
                        }
                    }
                    // not reachable
                }
                case GOT_DOLLAR: {
                    switch (ch) {
                        case '$': {
                            builder.append(ch);
                            state = INITIAL;
                            continue;
                        }
                        case '{': {
                            start = i + 1;
                            nameStart = start;
                            state = GOT_OPEN_BRACE;
                            continue;
                        }
                        default: {
                            // invalid; emit and resume
                            builder.append('$').append(ch);
                            state = INITIAL;
                            continue;
                        }
                    }
                    // not reachable
                }
                case GOT_OPEN_BRACE: {
                    switch (ch) {
                        case ':':
                        case '}':
                        case ',': {
                            final String name = expression.substring(nameStart, i).trim();
                            if ("/".equals(name)) {
                                builder.append(File.separator);
                                state = ch == '}' ? INITIAL : RESOLVED;
                                continue;
                            } else if (":".equals(name)) {
                                builder.append(File.pathSeparator);
                                state = ch == '}' ? INITIAL : RESOLVED;
                                continue;
                            }
                            final String val = bundle.getString(name);
                            if (val != null) {
                                builder.append(val);
                                state = ch == '}' ? INITIAL : RESOLVED;
                                continue;
                            } else if (ch == ',') {
                                nameStart = i + 1;
                                continue;
                            } else if (ch == ':') {
                                start = i + 1;
                                state = DEFAULT;
                                continue;
                            } else {
                                builder.append(expression.substring(start - 2, i + 1));
                                state = INITIAL;
                                continue;
                            }
                        }
                        default: {
                            continue;
                        }
                    }
                    // not reachable
                }
                case RESOLVED: {
                    if (ch == '}') {
                        state = INITIAL;
                    }
                    continue;
                }
                case DEFAULT: {
                    if (ch == '}') {
                        state = INITIAL;
                        builder.append(expression.substring(start, i));
                    }
                    continue;
                }
                default:
                    throw new IllegalStateException();
            }
        }
        switch (state) {
            case GOT_DOLLAR: {
                builder.append('$');
                break;
            }
            case DEFAULT:
            case GOT_OPEN_BRACE: {
                builder.append(expression.substring(start - 2));
                break;
            }
        }
        return builder.toString();
    }
}
