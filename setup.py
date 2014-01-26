from setuptools import setup, find_packages


scripts = [
    'clipseq/kmer_extractor.py',
    'clipseq/perform_idr.py',
    'clipseq/run_piranha.py',
    'editing/create_rna_editing_makefile.py',
    'general/calculate_NRF.py',
    'general/count_aligned_from_sam.py',
    'general/make_trackhubs.py',
    'general/negBedGraph.py',
    'general/normalize_bedGraph.py',
    'general/parsers.py',
    'mapping/map_paired_with_STAR.py',
    'mapping/sam_to_bam_and_sort.py',
    'miso/submit_index_gff.py',
    'miso/submit_miso_pipeline.py',
    'output_parsers/parseMiso.py',
    'riboseq/riboseq_coverage.py',
    'rnaseq/count_tags.py',
    'rnaseq/make_rnaseqc.py',
    'rnaseq/oldsplice.py', 
    'rnaseq/oldsplice_gff.py',
    'rnaseq/parse_oldsplice.py',
    'rnaseq/single_RPKM.py',
    'rnaseq/submit_oldsplice.py',
    'rnaseq/submit_oldsplice_gff.py',
    'rnaseq/submit_parse_oldsplice.py',
           ]

scripts = map((lambda x: "gscripts/" + x), scripts)

with open("README.rst") as file:
    long_description = file.read()

setup(

    name="gscripts",
    long_description=long_description,
    version="0.1.5",
    packages=find_packages(),


    install_requires=['setuptools',
                      'pysam >= 0.6',
                      'numpy >= 1.5.1 ',
                      'scipy >= 0.11.0',
                      'matplotlib >= 1.1.0',
                      'pybedtools >= 0.5',
                      'scikit-learn >= 0.13.0',
    ],

    setup_requires=["setuptools_git >= 0.3", ],

    scripts=scripts,

    #metadata for upload to PyPI
    author="Gabriel Pratt",
    author_email="gpratt@ucsd.edu",
    description="A set of scripts for analysis of high throughput data",
    license="GPL2",
    keywords="bioinformatics",
    url="https://github.com/gpratt",

    #Other stuff I feel like including here
    include_package_data=True,
    zip_safe=False #True I think
)
