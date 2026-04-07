package com.stilog.prism.comparevpi.view.tree;

import javax.swing.tree.DefaultMutableTreeNode;

import com.stilog.prism.comparevpi.model.dto.MergeRequest;
import com.stilog.prism.vpimodel.objects.Entity;
import com.stilog.prism.vpimodel.objects.Parameters;
import com.stilog.prism.vpimodel.vpsettings.FileDatas;

/**
* Factory pour créer des MergeRequest à partir de la structure de l'arbre
*/
public class MergeRequestFactory {
 
 /**
  * Crée un MergeRequest pour un merge simple
  */
 public MergeRequest createMergeRequest(DefaultMutableTreeNode node) {
     Object obj = node.getUserObject();
     
     if (obj instanceof Parameters) {
         DefaultMutableTreeNode entityNode = (DefaultMutableTreeNode) node.getParent();
         DefaultMutableTreeNode fileDatasNode = (DefaultMutableTreeNode) entityNode.getParent();
         
         return new MergeRequest((FileDatas) fileDatasNode.getUserObject(), (Entity) entityNode.getUserObject(), (Parameters) obj);
     }
     
     if (obj instanceof Entity) {
         DefaultMutableTreeNode fileDatasNode = (DefaultMutableTreeNode) node.getParent();
         
         return new MergeRequest(
             (FileDatas) fileDatasNode.getUserObject(),
             (Entity) obj
         );
     }
     
     throw new IllegalArgumentException("Type de nœud non supporté pour merge: " + obj.getClass());
 }
 
 /**
  * Crée un MergeRequest pour un replace
  */
 public MergeRequest createReplaceRequest(
         DefaultMutableTreeNode sourceNode, 
         DefaultMutableTreeNode targetNode) {
     
     Object source = sourceNode.getUserObject();
     Object target = targetNode.getUserObject();
     
     // Vérification de compatibilité
     if (!source.getClass().equals(target.getClass())) {
         throw new IllegalArgumentException(
             "Les types doivent correspondre pour un replace"
         );
     }
     
     if (source instanceof Parameters) {
         DefaultMutableTreeNode entityNode = (DefaultMutableTreeNode) sourceNode.getParent();
         DefaultMutableTreeNode fileDatasNode = (DefaultMutableTreeNode) entityNode.getParent();
         
         return new MergeRequest(
             (FileDatas) fileDatasNode.getUserObject(),
             (Entity) entityNode.getUserObject(),
             (Parameters) source,
             (Parameters) target
         );
     }
     
     if (source instanceof Entity) {
         DefaultMutableTreeNode fileDatasNode = (DefaultMutableTreeNode) sourceNode.getParent();
         
         return new MergeRequest(
             (FileDatas) fileDatasNode.getUserObject(),
             (Entity) source,
             (Entity) target
         );
     }
     
     throw new IllegalArgumentException("Type de nœud non supporté pour replace");
 }
}
