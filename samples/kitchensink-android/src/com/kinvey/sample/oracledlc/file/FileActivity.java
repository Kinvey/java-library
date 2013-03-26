package com.kinvey.sample.oracledlc.file;

import com.kinvey.sample.oracledlc.FeatureActivity;
import com.kinvey.sample.oracledlc.UseCaseFragment;

import java.util.Arrays;
import java.util.List;

/*
 * Copyright (c) 2013 Kinvey Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
public class FileActivity extends FeatureActivity {

    static final String FILENAME = "sample.txt";

    @Override
    public List<UseCaseFragment> getFragments() {
        return Arrays.asList(new UseCaseFragment[] {
                new UploadFragment(),
                new DownloadFragment(),
                new DeleteFragment()
        });
    }
}