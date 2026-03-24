Diagrams and how to render
==========================

Files:
- `objectaid_style.puml`: PlantUML source styled to resemble ObjectAid

Render options:

1) VS Code + PlantUML extension
   - Install `PlantUML` extension and Graphviz.
   - Open `objectaid_style.puml` and click preview or export PNG/SVG.

2) PlantUML jar (command line)
   - Download `plantuml.jar` and Graphviz (`dot`) on PATH.
   - Generate PNG:

```bash
java -jar plantuml.jar objectaid_style.puml
```

Notes:
- This diagram covers the main classes present in the repository and their
  high-level relationships (associations, implementations, factories).
- If you want an exact ObjectAid export, install the ObjectAid plugin in
  Eclipse and import the source; PlantUML here is a close visual approximation.
