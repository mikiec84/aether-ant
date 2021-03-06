/*******************************************************************************
 * Copyright (c) 2010, 2014 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.aether.internal.ant.tasks;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.model.Model;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Reference;
import org.eclipse.aether.internal.ant.AntRepoSys;
import org.eclipse.aether.internal.ant.types.Artifact;
import org.eclipse.aether.internal.ant.types.Artifacts;
import org.eclipse.aether.internal.ant.types.Pom;

/**
 */
public abstract class AbstractDistTask
    extends Task
{

    private Pom pom;

    private Artifacts artifacts;

    protected void validate()
    {
        getArtifacts().validate( this );

        Map<String, File> duplicates = new HashMap<String, File>();
        for ( Artifact artifact : getArtifacts().getArtifacts() )
        {
            String key = artifact.getType() + ':' + artifact.getClassifier();
            if ( "pom:".equals( key ) )
            {
                throw new BuildException( "You must not specify an <artifact> with type=pom"
                    + ", please use the <pom> element instead." );
            }
            else if ( duplicates.containsKey( key ) )
            {
                throw new BuildException( "You must not specify two or more artifacts with the same type ("
                    + artifact.getType() + ") and classifier (" + artifact.getClassifier() + ")" );
            }
            else
            {
                duplicates.put( key, artifact.getFile() );
            }

            validateArtifactGav( artifact );
        }

        Pom defaultPom = AntRepoSys.getInstance( getProject() ).getDefaultPom();
        if ( pom == null && defaultPom != null )
        {
            log( "Using default POM (" + defaultPom.getCoords() + ")", Project.MSG_INFO );
            pom = defaultPom;
        }

        if ( pom == null )
        {
            throw new BuildException( "You must specify the <pom file=\"...\"> element"
                + " to denote the descriptor for the artifacts" );
        }
        if ( pom.getFile() == null )
        {
            throw new BuildException( "You must specify a <pom> element that has the 'file' attribute set" );
        }
    }

    private void validateArtifactGav( Artifact artifact )
    {
        Pom artifactPom = artifact.getPom();
        if ( artifactPom != null )
        {
            String gid;
            String aid;
            String version;
            if ( artifactPom.getFile() != null )
            {
                Model model = artifactPom.getModel( this );
                gid = model.getGroupId();
                aid = model.getArtifactId();
                version = model.getVersion();
            }
            else
            {
                gid = artifactPom.getGroupId();
                aid = artifactPom.getArtifactId();
                version = artifactPom.getVersion();
            }
            
            Model model = getPom().getModel( this );
            
            if ( ! ( model.getGroupId().equals( gid ) && model.getArtifactId().equals( aid ) && model.getVersion().equals( version )) )
            {
                throw new BuildException( "Artifact references different pom than it would be installed with: "
                    + artifact.toString() );
            }
        }
    }

    protected Artifacts getArtifacts()
    {
        if ( artifacts == null )
        {
            artifacts = new Artifacts();
            artifacts.setProject( getProject() );
        }
        return artifacts;
    }

    public void addArtifact( Artifact artifact )
    {
        getArtifacts().addArtifact( artifact );
    }

    public void addArtifacts( Artifacts artifacts )
    {
        getArtifacts().addArtifacts( artifacts );
    }

    public void setArtifactsRef( Reference ref )
    {
        Artifacts artifacts = new Artifacts();
        artifacts.setProject( getProject() );
        artifacts.setRefid( ref );
        getArtifacts().addArtifacts( artifacts );
    }

    protected Pom getPom()
    {
        if ( pom == null )
        {
            return AntRepoSys.getInstance( getProject() ).getDefaultPom();
        }

        return pom;
    }

    public void addPom( Pom pom )
    {
        if ( this.pom != null )
        {
            throw new BuildException( "You must not specify multiple <pom> elements" );
        }
        this.pom = pom;
    }

    public void setPomRef( Reference ref )
    {
        if ( this.pom != null )
        {
            throw new BuildException( "You must not specify multiple <pom> elements" );
        }
        pom = new Pom();
        pom.setProject( getProject() );
        pom.setRefid( ref );
    }

}
