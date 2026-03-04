/*
 * Copyright 2021-2026 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.openaire.observatory.domain;

import org.springframework.security.core.Authentication;

import java.io.Serializable;
import java.util.Date;

public class Metadata implements Serializable {

    private Date creationDate;
    private String createdBy;
    private Date modificationDate;
    private String modifiedBy;

    public Metadata() {
        // no-arg constructor
    }

    public Metadata(Authentication authentication) {
        Date date = new Date();
        this.creationDate = date;
        this.modificationDate = date;
        this.createdBy = authentication != null ? User.getId(authentication) : "unknown";
        this.modifiedBy = this.createdBy;
    }

    public Metadata(Metadata metadata) {
        this.creationDate = new Date(metadata.getCreationDate().getTime());
        this.createdBy = metadata.getCreatedBy();
        this.modificationDate = new Date(metadata.getModificationDate().getTime());;
        this.modifiedBy = metadata.getModifiedBy();
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getModificationDate() {
        return modificationDate;
    }

    public void setModificationDate(Date modificationDate) {
        this.modificationDate = modificationDate;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    @Override
    public String toString() {
        return "Metadata{" +
                "creationDate=" + creationDate +
                ", createdBy='" + createdBy + '\'' +
                ", modificationDate=" + modificationDate +
                ", modifiedBy='" + modifiedBy + '\'' +
                '}';
    }
}
