/* 
 Copyright (C) GridGain Systems. All Rights Reserved.
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.compute.gridify.aop.spring;

import org.aopalliance.intercept.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.compute.gridify.*;
import org.gridgain.grid.*;
import org.gridgain.grid.compute.gridify.aop.*;
import org.gridgain.grid.util.typedef.*;

import java.lang.reflect.*;

import static org.gridgain.grid.GridGainState.*;

/**
 * Spring aspect that cross-cuts on all methods grid-enabled with
 * {@link Gridify} annotation and potentially executes them on
 * remote node.
 * <p>
 * Note that Spring uses proxy-based AOP, so in order to be properly
 * cross-cut, all methods need to be enhanced with {@link GridifySpringEnhancer}
 * helper.
 * <p>
 * See {@link Gridify} documentation for more information about execution of
 * {@code gridified} methods.
 * @see Gridify
 */
public class GridifySpringAspect implements MethodInterceptor {
    /**
     * Aspect implementation which executes grid-enabled methods on remote
     * nodes.
     *
     * {@inheritDoc}
     */
    @SuppressWarnings({"ProhibitedExceptionDeclared", "ProhibitedExceptionThrown", "CatchGenericClass", "unchecked"})
    @Override public Object invoke(MethodInvocation invoc) throws Throwable {
        Method mtd  = invoc.getMethod();

        Gridify ann = mtd.getAnnotation(Gridify.class);

        assert ann != null : "Intercepted method does not have gridify annotation.";

        // Since annotations in Java don't allow 'null' as default value
        // we have accept an empty string and convert it here.
        // NOTE: there's unintended behavior when user specifies an empty
        // string as intended grid name.
        // NOTE: the 'ann.gridName() == null' check is added to mitigate
        // annotation bugs in some scripting languages (e.g. Groovy).
        String gridName = F.isEmpty(ann.gridName()) ? null : ann.gridName();

        if (G.state(gridName) != STARTED)
            throw new GridException("Grid is not locally started: " + gridName);

        // Initialize defaults.
        GridifyArgument arg = new GridifyArgumentAdapter(mtd.getDeclaringClass(), mtd.getName(),
            mtd.getParameterTypes(), invoc.getArguments(), invoc.getThis());

        if (!ann.interceptor().equals(GridifyInterceptor.class)) {
            // Check interceptor first.
            if (!ann.interceptor().newInstance().isGridify(ann, arg))
                return invoc.proceed();
        }

        if (!ann.taskClass().equals(GridifyDefaultTask.class) && !ann.taskName().isEmpty())
            throw new GridException("Gridify annotation must specify either Gridify.taskName() or " +
                "Gridify.taskClass(), but not both: " + ann);

        try {
            Grid grid = G.grid(gridName);

            if (!ann.taskClass().equals(GridifyDefaultTask.class))
                return grid.compute().withTimeout(ann.timeout()).execute(
                    (Class<? extends GridComputeTask<GridifyArgument, Object>>)ann.taskClass(), arg).get();

            // If task name was not specified.
            if (ann.taskName().isEmpty())
                return grid.compute().withTimeout(ann.timeout()).execute(
                    new GridifyDefaultTask(invoc.getMethod().getDeclaringClass()), arg).get();

            // If task name was specified.
            return grid.compute().withTimeout(ann.timeout()).execute(ann.taskName(), arg).get();
        }
        catch (Exception e) {
            for (Class<?> ex : invoc.getMethod().getExceptionTypes()) {
                // Descend all levels down.
                Throwable cause = e.getCause();

                while (cause != null) {
                    if (ex.isAssignableFrom(cause.getClass()))
                        throw cause;

                    cause = cause.getCause();
                }

                if (ex.isAssignableFrom(e.getClass()))
                    throw e;
            }

            throw new GridifyRuntimeException("Undeclared exception thrown: " + e.getMessage(), e);
        }
    }
}
