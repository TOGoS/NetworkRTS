require 'digest/sha1'

require 'rubygems'
require 'base32'

puts "package togos.networkrts.cereal;"

class Opcode
  def initialize( name, urn )
    @name = name
    @urn = urn
  end
  
  attr_reader :name, :urn
end

opcodes = []

for file in Dir.glob('Opcodes/*.txt')
  basename = File.basename(file)
  if basename =~ /\.txt/
    opcode_name = $`
    sha1 = Digest::SHA1.file(file).digest
    sha1_urn = 'urn:sha1:'+Base32.encode(sha1)
    opcodes << Opcode.new( opcode_name, sha1_urn )
  end
end

opcodes.sort! { |o1,o2| o1.name <=> o2.name }

puts
for opcode in opcodes
  puts "import togos.networkrts.cereal.ops.#{opcode.name};"
end

puts
puts "class Opcodes\n{"
#for opcode in opcodes
#	puts "\tpublic static final #{opcode.name}
#end
puts "\tpublic static final Map<String,OpcodeBehavior> opcodes = new HashMap<String,OpcodeBehavior>;"
puts "\tstatic {\n"
for opcode in opcodes
  puts "\t\topcodes.put(#{opcode.urn.inspect}, #{opcode.name}.INSTANCE);" 
end
puts "\t}"
puts "}"
