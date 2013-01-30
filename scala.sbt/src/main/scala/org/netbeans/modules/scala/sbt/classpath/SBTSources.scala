package org.netbeans.modules.scala.sbt.classpath

import scala.collection.mutable.ArrayBuffer

import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import org.netbeans.modules.scala.sbt.project.ProjectConstants
import javax.swing.event.EventListenerList
import org.netbeans.api.project.Project
import org.netbeans.api.project.SourceGroup
import org.netbeans.api.project.Sources
import org.netbeans.spi.project.support.GenericSources
import org.openide.filesystems.FileObject
import org.openide.util.NbBundle

/**
 * 
 * @author Caoyuan Deng
 */
class SBTSources(project: Project) extends Sources with PropertyChangeListener {
  
  private val changeListeners = new EventListenerList
  private val changeEvent = new ChangeEvent(this)
  
  private var isSbtControllerListenerAdded = false

  override 
  def getSourceGroups(tpe: String): Array[SourceGroup] = {
    if (!isSbtControllerListenerAdded) {
      val sbtController = project.getLookup.lookup(classOf[SBTController])
      if (sbtController != null) {
        isSbtControllerListenerAdded = true
        sbtController.addPropertyChangeListener(this)
      }
    }
    
    tpe match {
      case Sources.TYPE_GENERIC =>
        // It's necessary for project's PhysicalView (in Files window), 
        // @see org.netbeans.modules.project.ui.PhysicalView#createNodesForProject(Project)
        val projectDir = project.getProjectDirectory
        Array(GenericSources.group(project, projectDir, projectDir.getNameExt, projectDir.getNameExt, null, null))
      case ProjectConstants.SOURCES_TYPE_JAVA =>
        val groups = new ArrayBuffer[SourceGroup]()
        maybeAddGroup(groups, tpe, false)
        maybeAddGroup(groups, tpe, true)
        groups.toArray
      case ProjectConstants.SOURCES_TYPE_SCALA =>
        val groups = new ArrayBuffer[SourceGroup]()
        maybeAddGroup(groups, tpe, false)
        maybeAddGroup(groups, tpe, true)
        groups.toArray
      case _ =>
        Array[SourceGroup]()
    }
  }

  private def maybeAddGroup(groups: ArrayBuffer[SourceGroup], tpe: String, test: Boolean) {
    val sbtController = project.getLookup.lookup(classOf[SBTController])
    val roots = if (sbtController != null) {
      sbtController.getSources(tpe, test)
    } else {
      // best try
      tpe match {
        case ProjectConstants.SOURCES_TYPE_JAVA =>
          Array(project.getProjectDirectory.getFileObject("src/" + (if (test) "test" else "main") + "/java"))
        case ProjectConstants.SOURCES_TYPE_SCALA =>
          Array(project.getProjectDirectory.getFileObject("src/" + (if (test) "test" else "main") + "/scala"))
        case _ => 
          Array[FileObject]()
      }
    }
    
    val name = tpe match {
      case ProjectConstants.SOURCES_TYPE_JAVA =>
        if (test) ProjectConstants.NAME_JAVATESTSOURCE else ProjectConstants.NAME_JAVASOURCE
      case ProjectConstants.SOURCES_TYPE_SCALA =>
        if (test) ProjectConstants.NAME_SCALATESTSOURCE else ProjectConstants.NAME_SCALASOURCE
      case _ => 
        ProjectConstants.NAME_OTHERSOURCE
    }
    
    val displayName = tpe match {
      case ProjectConstants.SOURCES_TYPE_JAVA =>
        if (test) NbBundle.getMessage(classOf[SBTSources], "SG_Test_JavaSources") else NbBundle.getMessage(classOf[SBTSources], "SG_JavaSources")
      case ProjectConstants.SOURCES_TYPE_SCALA =>
        if (test) NbBundle.getMessage(classOf[SBTSources], "SG_Test_ScalaSources") else NbBundle.getMessage(classOf[SBTSources], "SG_ScalaSources")
      case _ =>
        NbBundle.getMessage(classOf[SBTSources], "SG_OtherSources")
    }
    
    groups ++= {for (root <- roots if root != null) yield GenericSources.group(project, root, name, displayName, null, null)}
  }

  override 
  def addChangeListener(l: ChangeListener) {
    changeListeners.add(classOf[ChangeListener], l)
  }

  override 
  def removeChangeListener(l: ChangeListener) {
    changeListeners.remove(classOf[ChangeListener], l)
  }

  def propertyChange(evt: PropertyChangeEvent) {
    evt.getPropertyName match {
      case SBTController.SBT_LIBRARY_RESOLVED =>
        for (l <- changeListeners.getListeners(classOf[ChangeListener])) l.stateChanged(changeEvent)
      case _ =>
    }
  }
}
