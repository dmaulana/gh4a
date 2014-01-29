/*
 * Copyright 2011 Azwan Adli Abdullah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh4a.fragment;

import org.eclipse.egit.github.core.Repository;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.devspark.progressfragment.ProgressFragment;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.CollaboratorListActivity;
import com.gh4a.activities.ContributorListActivity;
import com.gh4a.activities.DownloadsActivity;
import com.gh4a.activities.ReleaseListActivity;
import com.gh4a.activities.WatcherListActivity;
import com.gh4a.activities.WikiListActivity;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.ReadmeLoader;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.github.mobile.util.HtmlUtils;
import com.github.mobile.util.HttpImageGetter;

public class RepositoryFragment extends ProgressFragment implements OnClickListener {
    private Repository mRepository;
    private View mContentView;

    private LoaderCallbacks<String> mReadmeCallback = new LoaderCallbacks<String>() {
        @Override
        public Loader<LoaderResult<String>> onCreateLoader(int id, Bundle args) {
            return new ReadmeLoader(getActivity(),
                    mRepository.getOwner().getLogin(), mRepository.getName());
        }
        @Override
        public void onResultReady(LoaderResult<String> result) {
            View v = getView();
            fillReadme(result.getData());
            v.findViewById(R.id.pb_readme).setVisibility(View.GONE);
            v.findViewById(R.id.readme).setVisibility(View.VISIBLE);
        }
    };

    public static RepositoryFragment newInstance(Repository repository) {
        RepositoryFragment f = new RepositoryFragment();

        Bundle args = new Bundle();
        args.putSerializable("REPOSITORY", repository);
        f.setArguments(args);
        
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepository = (Repository) getArguments().getSerializable("REPOSITORY");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.repository, null);
        return super.onCreateView(inflater, container, savedInstanceState);
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setContentView(mContentView);
        fillData();
        setContentShownNoAnimation(true);

        getLoaderManager().initLoader(0, null, mReadmeCallback);
    }

    private void fillData() {
        final Gh4Application app = Gh4Application.get(getActivity());

        UiUtils.assignTypeface(mContentView, app.boldCondensed, new int[] {
            R.id.readme_title, R.id.tv_login, R.id.tv_divider, R.id.tv_name,
            R.id.tv_stargazers_count, R.id.tv_forks_count, R.id.tv_issues_count,
            R.id.tv_pull_requests_count, R.id.tv_wiki_label, R.id.tv_contributors_label,
            R.id.tv_collaborators_label, R.id.other_info, R.id.tv_downloads_label,
            R.id.tv_releases_label
        });
        UiUtils.assignTypeface(mContentView, app.italic, new int[] {
            R.id.tv_parent
        });
        UiUtils.assignTypeface(mContentView, app.regular, new int[] {
            R.id.tv_desc, R.id.tv_language, R.id.tv_url
        });
        
        TextView tvOwner = (TextView) mContentView.findViewById(R.id.tv_login);
        tvOwner.setText(mRepository.getOwner().getLogin());
        tvOwner.setOnClickListener(this);

        TextView tvRepoName = (TextView) mContentView.findViewById(R.id.tv_name);
        tvRepoName.setText(mRepository.getName());
        
        TextView tvParentRepo = (TextView) mContentView.findViewById(R.id.tv_parent);
        if (mRepository.isFork()) {
            tvParentRepo.setVisibility(View.VISIBLE);

            Repository parent = mRepository.getParent();
            if (parent != null) {
                tvParentRepo.setText(app.getString(R.string.forked_from,
                        parent.getOwner().getLogin() + "/" + parent.getName()));
                tvParentRepo.setOnClickListener(this);
                tvParentRepo.setTag(parent);
            }
        } else {
            tvParentRepo.setVisibility(View.GONE);
        }

        fillTextView(R.id.tv_desc, 0, mRepository.getDescription());
        fillTextView(R.id.tv_language,R.string.repo_language, mRepository.getLanguage());
        fillTextView(R.id.tv_url, 0, mRepository.getHtmlUrl());

        mContentView.findViewById(R.id.cell_stargazers).setOnClickListener(this);
        mContentView.findViewById(R.id.cell_forks).setOnClickListener(this);
        mContentView.findViewById(R.id.cell_pull_requests).setOnClickListener(this);
        mContentView.findViewById(R.id.tv_wiki_label).setOnClickListener(this);
        mContentView.findViewById(R.id.tv_contributors_label).setOnClickListener(this);
        mContentView.findViewById(R.id.tv_collaborators_label).setOnClickListener(this);
        mContentView.findViewById(R.id.other_info).setOnClickListener(this);
        mContentView.findViewById(R.id.tv_downloads_label).setOnClickListener(this);
        mContentView.findViewById(R.id.tv_releases_label).setOnClickListener(this);
        
        TextView tvStargazersCount = (TextView) mContentView.findViewById(R.id.tv_stargazers_count);
        tvStargazersCount.setText(String.valueOf(mRepository.getWatchers()));
        
        TextView tvForksCount = (TextView) mContentView.findViewById(R.id.tv_forks_count);
        tvForksCount.setText(String.valueOf(mRepository.getForks()));
        
        TextView tvIssues = (TextView) mContentView.findViewById(R.id.tv_issues_label);
        TextView tvIssuesCount = (TextView) mContentView.findViewById(R.id.tv_issues_count);
        LinearLayout llIssues = (LinearLayout) mContentView.findViewById(R.id.cell_issues);
        
        if (mRepository.isHasIssues()) {
            llIssues.setVisibility(View.VISIBLE);
            llIssues.setOnClickListener(this);
            
            tvIssues.setVisibility(View.VISIBLE);
            
            tvIssuesCount.setText(String.valueOf(mRepository.getOpenIssues()));
            tvIssuesCount.setVisibility(View.VISIBLE);
        } else {
            llIssues.setVisibility(View.GONE);
            tvIssues.setVisibility(View.GONE);
            tvIssuesCount.setVisibility(View.GONE);
        }
        
        if (!mRepository.isHasWiki()) {
            mContentView.findViewById(R.id.tv_wiki_label).setVisibility(View.GONE);
        }
    }

    private void fillTextView(int id, int stringId, String text) {
        TextView view = (TextView) mContentView.findViewById(id);
        
        if (!StringUtils.isBlank(text)) {
            view.setText(stringId != 0 ? getString(stringId, text) : text);
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    public void updateStargazerCount(boolean starring) {
        TextView tvStargazersCount = (TextView) getView().findViewById(R.id.tv_stargazers_count);
        if (starring) {
            mRepository.setWatchers(mRepository.getWatchers() + 1);
        } else {
            mRepository.setWatchers(mRepository.getWatchers() - 1);
        }
        tvStargazersCount.setText(String.valueOf(mRepository.getWatchers()));
    }
    
    public void fillReadme(String readme) {
        TextView tvReadme = (TextView) getView().findViewById(R.id.readme);
        if (readme != null) {
            tvReadme.setMovementMethod(LinkMovementMethod.getInstance());

            readme = HtmlUtils.format(readme).toString();
            HttpImageGetter imageGetter = new HttpImageGetter(getActivity());
            imageGetter.bind(tvReadme, readme, mRepository.getId());
        } else {
            tvReadme.setText(R.string.repo_no_readme);
            tvReadme.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
        }
    }
    
    @Override
    public void onClick(View view) {
        int id = view.getId();
        String owner = mRepository.getOwner().getLogin();
        String name = mRepository.getName();
        Intent intent = null;

        if (id == R.id.tv_login) {
            IntentUtils.openUserInfoActivity(getActivity(), mRepository.getOwner());
        } else if (id == R.id.cell_pull_requests) {
            IntentUtils.openPullRequestListActivity(getActivity(), owner, name,
                    Constants.Issue.STATE_OPEN);
        } else if (id == R.id.tv_contributors_label) {
            intent = new Intent(getActivity(), ContributorListActivity.class);
        } else if (id == R.id.tv_collaborators_label) {
            intent = new Intent(getActivity(), CollaboratorListActivity.class);
        } else if (id == R.id.cell_issues) {
            IntentUtils.openIssueListActivity(getActivity(), owner, name,
                    Constants.Issue.STATE_OPEN);
        } else if (id == R.id.cell_stargazers) {
            intent = new Intent(getActivity(), WatcherListActivity.class);
            intent.putExtra("pos", 0);
        } else if (id == R.id.cell_forks) {
            intent = new Intent(getActivity(), WatcherListActivity.class);
            intent.putExtra("pos", 2);
        } else if (id == R.id.tv_wiki_label) {
            intent = new Intent(getActivity(), WikiListActivity.class);
        } else if (id == R.id.tv_downloads_label) {
            intent = new Intent(getActivity(), DownloadsActivity.class);
        } else if (id == R.id.tv_releases_label) {
            intent = new Intent(getActivity(), ReleaseListActivity.class);
        } else if (view.getTag() instanceof Repository) {
            Repository repo = (Repository) view.getTag();
            IntentUtils.openRepositoryInfoActivity(getActivity(), repo);
        }

        if (intent != null) {
            intent.putExtra(Constants.Repository.OWNER, owner);
            intent.putExtra(Constants.Repository.NAME, name);
            startActivity(intent);
        }
    }
}