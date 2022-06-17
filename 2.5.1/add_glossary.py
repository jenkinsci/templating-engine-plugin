import mkdocs_gen_files
import os
import glob

# iterate over pages and append glossary
for file in glob.glob("/docs/docs/**/*.md", recursive = True):
  if file.endswith('.md'):
    text = open(file).read()
    with mkdocs_gen_files.open(file.replace('/docs/docs/', ''), "w") as f:
      print(text + '\n--8<-- "./glossary.md"', file=f)