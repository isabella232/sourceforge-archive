#
#	Generate the source for BuildJetty.java.
#	used to simplify the Jetty build.
#
#	Original command line:
#
#	find src -type \*.java | 
#		sed -e 's/\.java$/.class,/' -e 's,^src/,,' | 
#		tr / .
#

use strict;
use File::Find ();

my $buildclassname  = shift 
	or die "Need a build class name\n";
my $sourcedirectory = shift
	or die "Need the base source directory\n";

my %classes = ();

sub wanted {
	my $name = $File::Find::name;
	# just look at the *.java files
	return 1 if -d or (not m{\.java\z}i);
	# translate the file name into a class name
	my $class = $name;
	$class =~ s{^$sourcedirectory/}{}io;
	$class =~ s{\.java\z}{}i;
	$class =~ y{/}{.};
	# is this a public class?
	my $leaf = $_;
	$leaf =~ s{\.java\z}{}i;
	open FILE, $_
		or die "ERROR($!) Cannot open file: $_\n";
	$classes{$class} = $name
		if grep /public\W.*class\W+$leaf\W/, <FILE>;
	close FILE;
}

File::Find::find({wanted => \&wanted}, $sourcedirectory);

print "class $buildclassname { Class[] classes = {\n";

my %packages = ();
my $class;

foreach $class (sort keys %classes) {
	if ($class =~  m{\.}) {
		my $package = $class;
		$package =~ s{\.[^.]*\z}{};
		if (not defined $packages{$package}) {
#			print "    // PACKAGE $package\n";
			$packages{$package} = 1;
		}
	}
	print "    $class\.class,\n";
}

print "};}\n";

