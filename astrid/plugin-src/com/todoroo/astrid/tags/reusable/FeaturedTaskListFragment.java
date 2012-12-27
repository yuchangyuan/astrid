package com.todoroo.astrid.tags.reusable;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.timsu.astrid.R;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.service.Autowired;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.actfm.TagViewFragment;
import com.todoroo.astrid.activity.TaskListActivity;
import com.todoroo.astrid.adapter.TaskAdapter;
import com.todoroo.astrid.api.Filter;
import com.todoroo.astrid.dao.TaskDao.TaskCriteria;
import com.todoroo.astrid.data.TagData;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.helper.AsyncImageView;
import com.todoroo.astrid.service.StatisticsConstants;
import com.todoroo.astrid.service.StatisticsService;
import com.todoroo.astrid.service.TagDataService;
import com.todoroo.astrid.tags.TagFilterExposer;
import com.todoroo.astrid.tags.TagService.Tag;
import com.todoroo.astrid.utility.Flags;

public class FeaturedTaskListFragment extends TagViewFragment {

    @Autowired private TagDataService tagDataService;

    private static final int MENU_CLONE_LIST = R.string.actfm_feat_list_clone;

    @Override
    protected TaskAdapter createTaskAdapter(TodorooCursor<Task> cursor) {
        return new ReusableTaskAdapter(this, R.layout.reusable_task_adapter_row,
                cursor, sqlQueryTemplate, false, null);
    }

    @Override
    protected void setupQuickAddBar() {
        super.setupQuickAddBar();
        quickAddBar.setVisibility(View.GONE);
        ((TextView) getView().findViewById(android.R.id.empty)).setOnClickListener(null);
    }

    @Override
    public void onTaskListItemClicked(long taskId, boolean editable) {
        // Do nothing
    }

    @Override
    protected int getTaskListBodyLayout() {
        return R.layout.task_list_body_featured_list;
    }

    @Override
    protected void addMenuItems(Menu menu, Activity activity) {
        super.addMenuItems(menu, activity);
        MenuItem item = menu.add(Menu.NONE, MENU_CLONE_LIST, 0, R.string.actfm_feat_list_clone);
        item.setIcon(R.drawable.ic_menu_list_copy);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean handleOptionsMenuItemSelected(int id, Intent intent) {
        if (id == MENU_CLONE_LIST) {
            cloneList();
            return true;
        }
        return super.handleOptionsMenuItemSelected(id, intent);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        // Do nothing
    }

    @Override
    protected void setUpMembersGallery() {
        // Repurposed this method to set up the description view
        AsyncImageView imageView = (AsyncImageView) getView().findViewById(R.id.url_image);
        String imageUrl = tagData.getValue(TagData.PICTURE);
        if (!TextUtils.isEmpty(imageUrl)) {
            imageView.setVisibility(View.VISIBLE);
            imageView.setDefaultImageResource(R.drawable.default_list_0);
            imageView.setUrl(imageUrl);
        } else {
            imageView.setVisibility(View.GONE);
        }

        final String description = tagData.getValue(TagData.TAG_DESCRIPTION);
        final Resources r = getActivity().getResources();
        TextView desc = (TextView) getView().findViewById(R.id.feat_list_desc);
        desc.setText(description);
        desc.setLines(4);
        desc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogUtilities.okDialog(getActivity(), r.getString(R.string.DLG_information_title),
                        0, description, null);
            }
        });
    }

    private void cloneList() {
        // Clone list
        if (taskAdapter == null || taskAdapter.getCount() == 0) {
            Toast.makeText(getActivity(), R.string.actfm_feat_list_clone_empty, Toast.LENGTH_LONG).show();
            return;
        }

        StatisticsService.reportEvent(StatisticsConstants.FEATURED_LIST_CLONED);
        final String localName = tagData.getValue(TagData.NAME) + " " + getString(R.string.actfm_feat_list_suffix); //$NON-NLS-1$
        long remoteId = 0;
        TodorooCursor<TagData> existing = tagDataService.query(Query.select(TagData.REMOTE_ID)
                .where(TagData.NAME.eqCaseInsensitive(localName)));
        try {
            if (existing.getCount() > 0) {
                existing.moveToFirst();
                TagData match = new TagData(existing);
                remoteId = match.getValue(TagData.REMOTE_ID);
            }

        } finally {
            existing.close();
        }

        final ProgressDialog pd = DialogUtilities.progressDialog(getActivity(), getString(R.string.actfm_feat_list_cloning));

        final long finalRemoteId = remoteId;
        new Thread(new Runnable() {
            @Override
            public void run() {
                final TodorooCursor<Task> tasks = taskService.fetchFiltered(taskAdapter.getQuery(), null, Task.PROPERTIES);
                try {
                    Task t = new Task();
                    for (tasks.moveToFirst(); !tasks.isAfterLast(); tasks.moveToNext()) {
                        t.readFromCursor(tasks);
                        taskService.cloneReusableTask(t,
                                localName, finalRemoteId);
                    }
                    final Activity activity = getActivity();
                    if (activity != null) {
                        DialogUtilities.dismissDialog(activity, pd);
                        DialogUtilities.okDialog(activity, getString(R.string.actfm_feat_list_clone_success), null);
                    }

                    Flags.set(Flags.REFRESH);
                    if (activity instanceof TaskListActivity) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TaskListActivity tla = (TaskListActivity) activity;
                                tla.setFilterMode(TaskListActivity.FILTER_MODE_NORMAL);

                                Filter clonedFilter;
                                Tag tag = new Tag(localName, tasks.getCount(), finalRemoteId);
                                clonedFilter = TagFilterExposer.filterFromTag(activity, tag, TaskCriteria.activeAndVisible());

                                tla.onFilterItemClicked(clonedFilter);
                            }
                        });
                    }
                } finally {
                    tasks.close();
                }
            }
        }).start();
    }

    @Override
    protected void refresh() {
        loadTaskListContent(true);
        ((TextView)taskListView.findViewById(android.R.id.empty)).setText(R.string.TLA_no_items);
        setUpMembersGallery();
    }

}
