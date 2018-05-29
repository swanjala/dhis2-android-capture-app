package com.dhis2.usescases.jira;

import com.dhis2.data.dagger.PerFragment;

import dagger.Subcomponent;

/**
 * QUADRAM. Created by ppajuelo on 24/05/2018.
 */
@PerFragment
@Subcomponent(modules = JiraModule.class)
public interface JiraComponent {
    void inject(JiraFragment programFragment);
}
