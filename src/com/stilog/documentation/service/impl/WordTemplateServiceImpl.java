package com.stilog.documentation.service.impl;

import com.stilog.documentation.service.WordTemplateService;
import com.stilog.documentation.model.dto.TableData;
import com.stilog.documentation.model.dto.style.StyleInfo;
import com.stilog.documentation.exception.*;
import com.stilog.documentation.util.TableConverter;
import com.stilog.documentation.util.DocumentUtil;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.math.BigInteger;

public class WordTemplateServiceImpl implements WordTemplateService {
    
    @Override
    public XWPFDocument loadTemplate(String templatePath) {
        try (InputStream inputStream = DocumentUtil.getResourceAsStream(templatePath)) {
            return new XWPFDocument(inputStream);
        } catch (IOException e) {
            throw new DocumentProcessingException(
                "Erreur lors du chargement du template Word", e);
        }
    }
    
    @Override
    public void insertTable(XWPFDocument document, TableData tableData, List<String> titleHierarchy) {
        try {
            // Trouver ou créer la hiérarchie de titres
            XWPFParagraph lastHeading = findOrCreateTitleHierarchy(document, titleHierarchy);
            
            // Ajouter un saut de ligne (paragraphe vide) avant le tableau
            XWPFParagraph emptyParagraph = document.createParagraph();
            // Optionnel : définir un espacement
            emptyParagraph.setSpacingBefore(200); // Espacement avant en twips (200 = ~0.35cm)
            emptyParagraph.setSpacingAfter(100);  // Espacement après en twips (100 = ~0.18cm)
            
            
            // Remplir le tableau avec les données
            XWPFTable table = TableConverter.convertToWordTable(document, tableData);
            
            // Appliquer les styles si disponibles
            if (tableData.getStyleInfo() != null) {
                applyTableStyle(table, tableData.getStyleInfo());
            }
            
            // Déplacer le tableau juste après le titre (si un titre a été créé)
            if (lastHeading != null) {
                //moveTableAfterParagraph(document, table, lastHeading);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new DocumentProcessingException(
                "Erreur lors de l'insertion du tableau: " + tableData.getTableName(), e);
        }
    }
    
    /**
     * Trouve ou crée la hiérarchie de titres et retourne le dernier paragraphe créé
     */
    private XWPFParagraph findOrCreateTitleHierarchy(XWPFDocument document, List<String> titleHierarchy) {
        if (titleHierarchy == null || titleHierarchy.isEmpty()) {
            return null;
        }
        
        XWPFParagraph lastParagraph = null;
        List<Integer> sectionNumbers = new ArrayList<>();
        
        for (int level = 0; level < titleHierarchy.size(); level++) {
            String titleText = titleHierarchy.get(level);
            int headingLevel = level + 1;
            
            // Construire la numérotation
            if (sectionNumbers.size() <= level) {
                sectionNumbers.add(1);
            }
            
            // Chercher si ce titre existe déjà
            XWPFParagraph existingHeading = findHeading(document, titleText, headingLevel);
            
            if (existingHeading != null) {
                lastParagraph = existingHeading;
            } else {
                // Créer le nouveau titre
                lastParagraph = document.createParagraph();
                lastParagraph.setStyle("Titre" + headingLevel);
                
                XWPFRun run = lastParagraph.createRun();
                run.setText(titleText);
                run.setBold(true);
                
                // Taille de police selon le niveau
                int fontSize = Math.max(10, 16 - (headingLevel * 2));
                run.setFontSize(fontSize);
            }
        }
        
        return lastParagraph;
    }
    
    /**
     * Cherche un titre dans le document
     */
    private XWPFParagraph findHeading(XWPFDocument document, String titleText, int headingLevel) {
        for (XWPFParagraph para : document.getParagraphs()) {
            String style = para.getStyle();
            String text = para.getText().trim();
            
            // Vérifier si le texte correspond
            if (text.toLowerCase().equals(titleText.toLowerCase())) {
                // Vérifier plusieurs styles possibles (anglais et français)
                if (style != null) {
                    String styleUpper = style.toUpperCase();
                    
                    // Styles anglais: Heading1, Heading2, etc.
                    if (style.equals("Heading" + headingLevel)) {
                        return para;
                    }
                    
                    // Styles français: Titre1, Titre2, etc.
                    if (style.equals("Titre" + headingLevel)) {
                        return para;
                    }
                    
                    // Styles avec espace: Titre 1, Titre 2, etc.
                    if (style.equals("Titre " + headingLevel)) {
                        return para;
                    }
                    
                    // Vérifier aussi en minuscules
                    if (style.equalsIgnoreCase("heading" + headingLevel) ||
                        style.equalsIgnoreCase("titre" + headingLevel) ||
                        style.equalsIgnoreCase("titre " + headingLevel)) {
                        return para;
                    }
                    
                    // Styles intégrés Word (IDs numériques)
                    if (styleUpper.contains("HEADING") || styleUpper.contains("TITRE")) {
                        return para;
                    }
                }
            }
        }
        
        return null;
    }
    
    @Override
    public void saveDocument(XWPFDocument document, String outputPath) {
    	//Ecraser le fichier si il existe deja
    	File file = new File(outputPath);
    	if(file.exists())
    		file.delete();
    	
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            document.write(fos);
            document.close();
        } catch (IOException e) {
            throw new DocumentProcessingException(
                "Erreur lors de la sauvegarde du document", e);
        }
    }
    
    private void applyTableStyle(XWPFTable table, StyleInfo styleInfo) {
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        if (tblPr == null) {
            tblPr = table.getCTTbl().addNewTblPr();
        }
        
        CTTblBorders borders = tblPr.addNewTblBorders();
        configureBorder(borders.addNewTop());
        configureBorder(borders.addNewBottom());
        configureBorder(borders.addNewLeft());
        configureBorder(borders.addNewRight());
        configureBorder(borders.addNewInsideH());
        configureBorder(borders.addNewInsideV());
        
        if (styleInfo.isHasHeaderRow() && table.getRows().size() > 0) {
            XWPFTableRow headerRow = table.getRow(0);
            for (XWPFTableCell cell : headerRow.getTableCells()) {
                cell.setColor(styleInfo.getHeaderBackgroundColor() != null ? 
                    styleInfo.getHeaderBackgroundColor().replace("#", "") : "4472C4");
                
                for (XWPFParagraph paragraph : cell.getParagraphs()) {
                    for (XWPFRun run : paragraph.getRuns()) {
                        run.setBold(true);
                        run.setColor("FFFFFF");
                    }
                }
            }
        }
    }
    
    private void configureBorder(CTBorder border) {
        border.setVal(STBorder.SINGLE);
        border.setSz(BigInteger.valueOf(4));
        border.setSpace(BigInteger.ZERO);
        border.setColor("000000");
    }
}