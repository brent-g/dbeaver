/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.impl;

import org.jkiss.dbeaver.Log;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jkiss.dbeaver.model.DBPApplication;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDContentStorageLocal;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * File content storage
 */
public class TemporaryContentStorage implements DBDContentStorageLocal {

    static final Log log = Log.getLog(TemporaryContentStorage.class);

    private final DBPApplication application;
    private IFile file;

    public TemporaryContentStorage(DBPApplication application, IFile file)
    {
        this.application = application;
        this.file = file;
    }

    @Override
    public InputStream getContentStream()
        throws IOException
    {
        try {
            return file.getContents();
        }
        catch (CoreException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Reader getContentReader()
        throws IOException
    {
        try {
            return new InputStreamReader(
                file.getContents(),
                file.getCharset());
        }
        catch (CoreException e) {
            throw new IOException(e);
        }
    }

    @Override
    public long getContentLength()
    {
        return file.getLocation().toFile().length();
    }

    @Override
    public String getCharset()
    {
        try {
            return file.getCharset();
        }
        catch (CoreException e) {
            log.warn(e);
            return null;
        }
    }

    @Override
    public DBDContentStorage cloneStorage(DBRProgressMonitor monitor)
        throws IOException
    {
        // Create new local storage
        IFile tempFile = ContentUtils.createTempContentFile(monitor, application, "copy" + this.hashCode());
        try {
            InputStream is = file.getContents(true);
            try {
                tempFile.setContents(is, true, false, monitor.getNestedMonitor());
            }
            finally {
                ContentUtils.close(is);
            }
        } catch (CoreException e) {
            ContentUtils.deleteTempFile(monitor, tempFile);
            throw new IOException(e);
        }
        return new TemporaryContentStorage(application, tempFile);
    }

    @Override
    public void release()
    {
        try {
            file.delete(true, false, new NullProgressMonitor());
        }
        catch (CoreException e) {
            log.warn(e);
        }
    }

    @Override
    public IFile getDataFile()
    {
        return file;
    }
}
