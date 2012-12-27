package com.todoroo.astrid.subtasks;

import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.core.CoreFilterExposer;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.subtasks.AstridOrderedListUpdater.Node;
import com.todoroo.astrid.test.DatabaseTestCase;

/**
 * Contains useful methods common to all subtasks tests
 * @author Sam
 *
 */
public class SubtasksTestCase extends DatabaseTestCase {

    protected SubtasksUpdater<String> updater;
    protected Filter filter;

    /* Starting State:
    *
    * A
    *  B
    *  C
    *   D
    * E
    * F
    */
    public static final String DEFAULT_SERIALIZED_TREE = "[-1, [1, 2, [3, 4]], 5, 6]".replaceAll("\\s", "");

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        filter = CoreFilterExposer.buildInboxFilter(getContext().getResources());
        Preferences.clear(SubtasksUpdater.ACTIVE_TASKS_ORDER);
        updater = new SubtasksFilterUpdater();
    }

    protected void expectParentAndPosition(Task task, Task parent, int positionInParent) {
        long parentId = (parent == null ? -1 : parent.getId());
        Node n = updater.findNodeForTask(task.getId());
        assertNotNull("No node found for task " + task.getValue(Task.TITLE), n);
        assertEquals("Parent mismatch", parentId, n.parent.taskId);
        assertEquals("Position mismatch", positionInParent, n.parent.children.indexOf(n));
    }

}
