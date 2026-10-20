# -*- coding: utf-8 -*-
"""
IntelliTrace AI - Complete Learning Guide Builder
Generates docs/IntelliTrace_AI_Complete_Learning_Guide.md and docs/IntelliTrace_AI_Complete_Learning_Guide.pdf
"""

import os
import re
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, KeepTogether, HRFlowable, Preformatted
)
from reportlab.pdfgen import canvas

from guide_data_part1 import CHAPTER_1, CHAPTER_2, CHAPTER_3, CHAPTER_4, CHAPTER_5, CHAPTER_6
from guide_data_part2 import CHAPTER_7, CHAPTER_8, CHAPTER_9, CHAPTER_10, CHAPTER_11, CHAPTER_12
from guide_data_part3 import CHAPTER_13, CHAPTER_14, CHAPTER_15, CHAPTER_16, CHAPTER_17, CHAPTER_18

class NumberedCanvas(canvas.Canvas):
    """
    Two-pass canvas to calculate total page count and add running headers and footers.
    """
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._saved_page_states = []

    def showPage(self):
        self._saved_page_states.append(dict(self.__dict__))
        self._startPage()

    def save(self):
        num_pages = len(self._saved_page_states)
        for state in self._saved_page_states:
            self.__dict__.update(state)
            self.draw_page_decorations(num_pages)
            super().showPage()
        super().save()

    def draw_page_decorations(self, page_count):
        if self._pageNumber == 1:
            return  # Suppress on title page

        self.saveState()
        self.setFont("Helvetica-Bold", 8)
        self.setFillColor(colors.HexColor("#718096"))

        # Running Header
        self.drawString(54, letter[1] - 36, "IntelliTrace AI — Full-Stack Log Analysis & Debugging Platform")
        self.setFont("Helvetica", 8)
        self.drawRightString(letter[0] - 54, letter[1] - 36, "Complete Learning & Interview Manual")
        self.setStrokeColor(colors.HexColor("#CBD5E0"))
        self.setLineWidth(0.75)
        self.line(54, letter[1] - 42, letter[0] - 54, letter[1] - 42)

        # Running Footer
        self.line(54, 45, letter[0] - 54, 45)
        self.drawString(54, 32, "Java 21 • Spring Boot 3.2.5 • React 18 • MySQL • JUnit 5 • AI Diagnostic Advisor")
        page_str = f"Page {self._pageNumber} of {page_count}"
        self.drawRightString(letter[0] - 54, 32, page_str)

        self.restoreState()


def build_markdown_file(chapters, output_path):
    print(f"Writing complete Markdown guide to {output_path}...")
    with open(output_path, "w", encoding="utf-8") as f:
        f.write("# IntelliTrace AI: Complete Application Learning & Technical Interview Guide\n\n")
        f.write("> **Comprehensive Technical Manual, Codebase Walkthrough, Architecture Reference, and Interview Preparation**\n")
        f.write("> *Covers: Java 21, Spring Boot 3.2.5, Spring Data JPA, Hibernate, MySQL, React 18, JUnit 5, and Responsible AI Integration*\n\n")
        f.write("---\n\n")
        f.write("## Table of Contents\n\n")
        for i in range(1, 19):
            titles = [
                "Project Introduction",
                "Complete System Architecture",
                "Project Folder Structure",
                "Java Fundamentals Used in the Project",
                "Java Log Parser",
                "Error Grouping and Analysis",
                "Spring Boot",
                "REST API Documentation",
                "JPA, Spring Data JPA, Hibernate, and MySQL",
                "React Frontend",
                "AI Integration",
                "Testing",
                "Running and Debugging the Project",
                "Codebase Walkthrough",
                "Project Explanation for Interviews",
                "Technical Interview Questions and Answers (100+ Q&A)",
                "Project Modification Exercises",
                "Quick Revision Sheet"
            ]
            f.write(f"- [CHAPTER {i}: {titles[i-1]}](#chapter-{i}-{titles[i-1].lower().replace(' ', '-').replace('&', '').replace(',', '').replace('(', '').replace(')', '').replace('+', '')})\n")
        f.write("\n---\n\n")

        for ch in chapters:
            f.write(ch.strip())
            f.write("\n\n---\n\n")
    print(f"Markdown guide successfully written ({os.path.getsize(output_path):,} bytes).")


def format_inline_markdown(text):
    if not text:
        return ""
    # 1. Escape literal ampersands
    text = re.sub(r'&(?!amp;|lt;|gt;|quot;|apos;)', '&amp;', text)
    # 2. Escape literal angle brackets before adding HTML tags
    text = text.replace('<', '&lt;').replace('>', '&gt;')
    # 3. Bold: **text** -> <b>text</b>
    text = re.sub(r'\*\*(.*?)\*\*', r'<b>\1</b>', text)
    # 4. Italic: *text* -> <i>text</i> (avoid matching inside <b>)
    text = re.sub(r'(?<!<b>)\*([^*]+?)\*(?!</b>)', r'<i>\1</i>', text)
    # 5. Inline code: `text` -> <font name="Courier">text</font>
    text = re.sub(r'`(.*?)`', r'<font name="Courier">\1</font>', text)
    return text


def parse_markdown_to_flowables(chapters, styles):
    story = []

    # Title Page
    story.append(Spacer(1, 40))
    title_style = ParagraphStyle(
        'DocTitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=28,
        leading=34,
        textColor=colors.HexColor('#1A365D'),
        alignment=1
    )
    subtitle_style = ParagraphStyle(
        'DocSubtitle',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=13,
        leading=18,
        textColor=colors.HexColor('#2B6CB0'),
        alignment=1
    )
    meta_style = ParagraphStyle(
        'DocMeta',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=9.5,
        leading=14,
        textColor=colors.HexColor('#4A5568'),
        alignment=1
    )

    story.append(Paragraph("IntelliTrace AI", title_style))
    story.append(Spacer(1, 8))
    story.append(Paragraph("AI-Powered Application Log Analysis & Debugging Platform", subtitle_style))
    story.append(Spacer(1, 6))
    story.append(Paragraph("Complete Technical Learning Manual, Codebase Architecture & 100+ Technical Interview Guide", meta_style))
    story.append(Spacer(1, 20))

    # Badge Box Table
    badge_data = [
        [
            Paragraph("<b>Backend Engine:</b> Java 21 LTS • Spring Boot 3.2.5", meta_style),
            Paragraph("<b>Persistence:</b> Spring Data JPA • Hibernate • MySQL / H2", meta_style)
        ],
        [
            Paragraph("<b>Frontend SPA:</b> React 18 • Vite • Axios • Glassmorphic CSS", meta_style),
            Paragraph("<b>AI Integration:</b> Google Gemini API (gemini-2.0-flash) • Heuristic Fallback", meta_style)
        ],
        [
            Paragraph("<b>Testing Suite:</b> JUnit 5 • Mockito • SpringBootTest (48 Tests Passed)", meta_style),
            Paragraph("<b>Architecture:</b> Modular Monolith • Controller-Service-Repository", meta_style)
        ]
    ]
    badge_table = Table(badge_data, colWidths=[240, 240])
    badge_table.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,-1), colors.HexColor('#F7FAFC')),
        ('BOX', (0,0), (-1,-1), 1, colors.HexColor('#CBD5E0')),
        ('INNERGRID', (0,0), (-1,-1), 0.5, colors.HexColor('#E2E8F0')),
        ('TOPPADDING', (0,0), (-1,-1), 6),
        ('BOTTOMPADDING', (0,0), (-1,-1), 6),
        ('LEFTPADDING', (0,0), (-1,-1), 8),
        ('RIGHTPADDING', (0,0), (-1,-1), 8),
    ]))
    story.append(badge_table)
    story.append(Spacer(1, 30))

    # Table of Contents
    story.append(Paragraph("<b>TABLE OF CONTENTS</b>", ParagraphStyle('TOCHeader', fontName='Helvetica-Bold', fontSize=14, textColor=colors.HexColor('#1A365D'))))
    story.append(HRFlowable(width="100%", thickness=1.5, color=colors.HexColor('#2B6CB0'), spaceBefore=4, spaceAfter=12))

    toc_items = [
        ("Chapter 1: Project Introduction", "Overview, Problem Statement, Objectives, Incident Scenario"),
        ("Chapter 2: Complete System Architecture", "Modular Monolith, Request Flow, Component Matrix, AI Flow"),
        ("Chapter 3: Project Folder Structure", "Complete File Tree, Module Hierarchy, Class Responsibilities"),
        ("Chapter 4: Java Fundamentals Used in the Project", "OOP, Encapsulation, Collections, Regex, Streams, Lambdas"),
        ("Chapter 5: Java Log Parser", "Streaming Parser, Multiline State Machine, Regex Breakdown, Complexity"),
        ("Chapter 6: Error Grouping and Analysis", "Normalization Rules, Variable Masking, Clustering Math"),
        ("Chapter 7: Spring Boot", "Annotations, Autoconfiguration, DI, Global Exception Handling"),
        ("Chapter 8: REST API Documentation", "All 7 Endpoints, Request/Response Payloads, Postman Guide"),
        ("Chapter 9: JPA, Spring Data JPA, Hibernate, MySQL", "ORM Architecture, Entities, Repositories, Dual DB Profiles"),
        ("Chapter 10: React Frontend", "Vite SPA, Component Architecture, Glassmorphism, Axios Layer"),
        ("Chapter 11: AI Integration", "Prompt Engineering, Sanitization, Heuristic Fallback Engine"),
        ("Chapter 12: Testing", "JUnit 5, Mockito, MockMvc, 40 Automated Tests Breakdown"),
        ("Chapter 13: Running and Debugging the Project", "Setup Guide, Environment Variables, Ports, Troubleshooting"),
        ("Chapter 14: Codebase Walkthrough", "Deep-Dive into 10 Core Application Classes & Components"),
        ("Chapter 15: Project Explanation for Interviews", "30-Sec Pitch, 1-Min Pitch, 2-Min Deep Dive, Talking Points"),
        ("Chapter 16: Technical Interview Questions & Answers", "100+ Real Interview Questions Across 15 Categories (A-O)"),
        ("Chapter 17: Project Modification Exercises", "15 Practical Coding Exercises with Hints & Full Solutions"),
        ("Chapter 18: Quick Revision Sheet", "One-Page Cheat Sheets for Java, Annotations, HTTP, SQL, Arch")
    ]

    toc_data = []
    for title, desc in toc_items:
        toc_data.append([
            Paragraph(f"<b>{title}</b>", ParagraphStyle('TOCTitle', fontName='Helvetica-Bold', fontSize=8.5, textColor=colors.HexColor('#2B6CB0'))),
            Paragraph(desc, ParagraphStyle('TOCDesc', fontName='Helvetica', fontSize=8, textColor=colors.HexColor('#4A5568')))
        ])
    toc_table = Table(toc_data, colWidths=[210, 290])
    toc_table.setStyle(TableStyle([
        ('LINEBELOW', (0,0), (-1,-1), 0.5, colors.HexColor('#EDF2F7')),
        ('TOPPADDING', (0,0), (-1,-1), 3),
        ('BOTTOMPADDING', (0,0), (-1,-1), 3),
    ]))
    story.append(toc_table)
    story.append(PageBreak())

    # Styling definitions for body
    h1_style = ParagraphStyle(
        'H1', fontName='Helvetica-Bold', fontSize=15, leading=19, textColor=colors.HexColor('#1A365D'), spaceBefore=14, spaceAfter=6, keepWithNext=True
    )
    h2_style = ParagraphStyle(
        'H2', fontName='Helvetica-Bold', fontSize=11.5, leading=15, textColor=colors.HexColor('#2B6CB0'), spaceBefore=10, spaceAfter=4, keepWithNext=True
    )
    h3_style = ParagraphStyle(
        'H3', fontName='Helvetica-Bold', fontSize=9.5, leading=13, textColor=colors.HexColor('#2D3748'), spaceBefore=8, spaceAfter=3, keepWithNext=True
    )
    body_style = ParagraphStyle(
        'Body', fontName='Helvetica', fontSize=8.5, leading=12, textColor=colors.HexColor('#2D3748'), spaceAfter=5
    )
    bullet_style = ParagraphStyle(
        'Bullet', fontName='Helvetica', fontSize=8.5, leading=12, textColor=colors.HexColor('#2D3748'), leftIndent=12, spaceAfter=3
    )
    code_style = ParagraphStyle(
        'CodeText', fontName='Courier', fontSize=7.5, leading=9.5, textColor=colors.HexColor('#1A202C')
    )
    qa_q_style = ParagraphStyle(
        'QA_Q', fontName='Helvetica-Bold', fontSize=8.5, leading=12, textColor=colors.HexColor('#1A365D'), spaceBefore=6, spaceAfter=2, keepWithNext=True
    )
    qa_a_style = ParagraphStyle(
        'QA_A', fontName='Helvetica', fontSize=8.5, leading=12, textColor=colors.HexColor('#2D3748'), leftIndent=8, spaceAfter=5
    )

    for ch_idx, chapter_text in enumerate(chapters):
        if ch_idx > 0:
            story.append(PageBreak())

        lines = chapter_text.strip().split('\n')
        in_code_block = False
        code_lines = []
        in_table = False
        table_rows = []

        for line in lines:
            line_str = line.strip()

            # Code block detection
            if line_str.startswith('```'):
                if in_code_block:
                    # Flush code block in chunks of 28 lines to allow page breaks
                    CHUNK_SIZE = 28
                    story.append(Spacer(1, 3))
                    if not code_lines:
                        code_lines = [""]
                    for ci in range(0, len(code_lines), CHUNK_SIZE):
                        chunk = code_lines[ci:ci + CHUNK_SIZE]
                        chunk_content = '\n'.join(chunk)
                        code_para = Preformatted(chunk_content, code_style)
                        code_table = Table([[code_para]], colWidths=[490])
                        code_table.setStyle(TableStyle([
                            ('BACKGROUND', (0,0), (-1,-1), colors.HexColor('#F7FAFC')),
                            ('BOX', (0,0), (-1,-1), 1, colors.HexColor('#CBD5E0')),
                            ('TOPPADDING', (0,0), (-1,-1), 3),
                            ('BOTTOMPADDING', (0,0), (-1,-1), 3),
                            ('LEFTPADDING', (0,0), (-1,-1), 6),
                            ('RIGHTPADDING', (0,0), (-1,-1), 6),
                        ]))
                        story.append(code_table)
                        if ci + CHUNK_SIZE < len(code_lines):
                            story.append(Spacer(1, 2))
                    story.append(Spacer(1, 4))
                    in_code_block = False
                    code_lines = []
                else:
                    in_code_block = True
                    code_lines = []
                continue

            if in_code_block:
                code_lines.append(line)
                continue

            # Markdown Table detection
            if line_str.startswith('|') and line_str.endswith('|'):
                # Check if it's separator row
                if re.match(r'^\|[\s\-:|]+\|$', line_str):
                    continue
                cells = [c.strip() for c in line_str.split('|')[1:-1]]
                table_rows.append(cells)
                in_table = True
                continue
            else:
                if in_table and table_rows:
                    # Flush table
                    col_count = len(table_rows[0])
                    col_w = 490 / col_count if col_count > 0 else 490
                    formatted_table_data = []
                    for r_idx, row in enumerate(table_rows):
                        row_cells = []
                        for cell in row:
                            formatted_cell = format_inline_markdown(cell)
                            if r_idx == 0:
                                p = Paragraph(f"<b>{formatted_cell}</b>", ParagraphStyle('TH', fontName='Helvetica-Bold', fontSize=8, textColor=colors.white))
                            else:
                                p = Paragraph(formatted_cell, ParagraphStyle('TD', fontName='Helvetica', fontSize=7.5, textColor=colors.HexColor('#2D3748')))
                            row_cells.append(p)
                        formatted_table_data.append(row_cells)

                    t = Table(formatted_table_data, colWidths=[col_w]*col_count)
                    t.setStyle(TableStyle([
                        ('BACKGROUND', (0,0), (-1,0), colors.HexColor('#2B6CB0')),
                        ('ALIGN', (0,0), (-1,-1), 'LEFT'),
                        ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor('#CBD5E0')),
                        ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.white, colors.HexColor('#F7FAFC')]),
                        ('TOPPADDING', (0,0), (-1,-1), 3),
                        ('BOTTOMPADDING', (0,0), (-1,-1), 3),
                        ('LEFTPADDING', (0,0), (-1,-1), 4),
                        ('RIGHTPADDING', (0,0), (-1,-1), 4),
                    ]))
                    story.append(Spacer(1, 4))
                    story.append(t)
                    story.append(Spacer(1, 4))
                    in_table = False
                    table_rows = []

            if not line_str:
                story.append(Spacer(1, 3))
                continue

            # Headings
            if line_str.startswith('# '):
                h1_text = format_inline_markdown(line_str[2:].strip())
                story.append(Spacer(1, 6))
                story.append(Paragraph(h1_text, h1_style))
                story.append(HRFlowable(width="100%", thickness=1.5, color=colors.HexColor('#2B6CB0'), spaceBefore=2, spaceAfter=8))
            elif line_str.startswith('## '):
                h2_text = format_inline_markdown(line_str[3:].strip())
                story.append(Paragraph(h2_text, h2_style))
            elif line_str.startswith('### '):
                h3_text = format_inline_markdown(line_str[4:].strip())
                story.append(Paragraph(h3_text, h3_style))
            elif line_str.startswith('- ') or line_str.startswith('* '):
                bullet_text = line_str[2:].strip()
                formatted = format_inline_markdown(bullet_text)
                story.append(Paragraph(f"• {formatted}", bullet_style))
            elif re.match(r'^\d+\.\s+', line_str):
                formatted = format_inline_markdown(line_str)
                story.append(Paragraph(formatted, qa_q_style))
            elif line_str.startswith('*Answer*:'):
                ans_text = line_str.replace('*Answer*:', '<b>Answer:</b> ')
                formatted = format_inline_markdown(ans_text)
                story.append(Paragraph(formatted, qa_a_style))
            else:
                formatted = format_inline_markdown(line_str)
                story.append(Paragraph(formatted, body_style))

    return story


def build_pdf(chapters, output_pdf_path):
    print(f"Building complete PDF guide with ReportLab to {output_pdf_path}...")
    doc = SimpleDocTemplate(
        output_pdf_path,
        pagesize=letter,
        leftMargin=54,
        rightMargin=54,
        topMargin=54,
        bottomMargin=54
    )
    styles = getSampleStyleSheet()
    story = parse_markdown_to_flowables(chapters, styles)
    doc.build(story, canvasmaker=NumberedCanvas)
    print(f"PDF guide successfully built ({os.path.getsize(output_pdf_path):,} bytes).")


if __name__ == '__main__':
    all_chapters = [
        CHAPTER_1, CHAPTER_2, CHAPTER_3, CHAPTER_4, CHAPTER_5, CHAPTER_6,
        CHAPTER_7, CHAPTER_8, CHAPTER_9, CHAPTER_10, CHAPTER_11, CHAPTER_12,
        CHAPTER_13, CHAPTER_14, CHAPTER_15, CHAPTER_16, CHAPTER_17, CHAPTER_18
    ]

    docs_dir = os.path.join(os.path.dirname(__file__), "docs")
    os.makedirs(docs_dir, exist_ok=True)

    md_path = os.path.join(docs_dir, "IntelliTrace_AI_Complete_Learning_Guide.md")
    pdf_path = os.path.join(docs_dir, "IntelliTrace_AI_Complete_Learning_Guide.pdf")

    build_markdown_file(all_chapters, md_path)
    build_pdf(all_chapters, pdf_path)
