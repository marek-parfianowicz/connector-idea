/**
 * Copyright (C) 2008 Atlassian
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.atlassian.theplugin.jira.api;

import java.util.Map;

public class JIRAVersionBean extends AbstractJIRAConstantBean {
	protected boolean isReleased = false;

	public JIRAVersionBean(Map<String, String> map) {
		super(map);
		isReleased = Boolean.valueOf(map.get("released"));
	}

	public JIRAVersionBean(long id, String name) {
		this.id = id;
		this.name = name;
	}

	public JIRAVersionBean(JIRAVersionBean parent) {
		this(parent.getMap());
	}

	public boolean isReleased() {
		return isReleased;
	}

	public void setReleased(boolean released)  {
		isReleased = released;
	}

	public String getQueryStringFragment() {
		return "version=" + getId();
	}

	public JIRAQueryFragment getClone() {
		return new JIRAVersionBean(this);
	}
}
