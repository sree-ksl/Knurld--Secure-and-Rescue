// Copyright 2016 Intellisis Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file
package com.ds.knurld;

public interface AsyncResponse {
    void processFinish(String method, String output);
    //void processFinish(DropboxItem dropboxItem);
}
